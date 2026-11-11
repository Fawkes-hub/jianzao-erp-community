/**
 * 图片上传前的浏览器端压缩工具
 *
 * 统一做“长边降采样 + JPEG 质量控制”，减少弱网上传耗时、后端 MultipartFile 内存占用和 OSS 存储。
 * 约束：
 * - SVG/GIF 不重编码：矢量图和动图重编码会丢失语义；
 * - 源图带透明通道时默认保留 PNG，避免透明区域转 JPEG 后变黑；
 * - 输出文件名/扩展名/MIME 保持一致，避免 TDesign 只上传 Blob 时后端拿到 "blob" 而解析错后缀；
 * - 解码、绘制、编码任何一步失败都回退原文件，压缩不能阻断上传。
 */

export type CompressImageMimeType = 'image/jpeg' | 'image/png';

export interface CompressImageOptions {
  /** 目标体积上限（KB），仅 JPEG 生效；<=0 表示只做尺寸压缩 */
  maxSizeKB?: number;
  /** 长边上限（px），超过则等比缩小；<=0 表示不限制 */
  maxEdge?: number;
  /** 小于该体积（KB）的文件直接跳过，避免无意义重编码；<=0 表示不跳过 */
  skipBelowKB?: number;
  /** 初始编码质量（0-1） */
  quality?: number;
  /** 输出格式 */
  mimeType?: CompressImageMimeType;
  /** 是否保留透明通道；true 时透明图自动输出 PNG */
  preserveAlpha?: boolean;
}

/** 默认目标体积（KB） */
export const DEFAULT_IMAGE_MAX_SIZE_KB = 300;

/** 默认长边上限（px） */
export const DEFAULT_IMAGE_MAX_EDGE = 1920;

/** 转 JPEG 时透明区域的填充色 */
const JPEG_BACKGROUND = '#ffffff';

/** JPEG 质量搜索下限 */
const MIN_JPEG_QUALITY = 0.5;

const DEFAULT_OPTIONS: Required<CompressImageOptions> = {
  maxSizeKB: DEFAULT_IMAGE_MAX_SIZE_KB,
  maxEdge: DEFAULT_IMAGE_MAX_EDGE,
  skipBelowKB: DEFAULT_IMAGE_MAX_SIZE_KB,
  quality: 0.85,
  mimeType: 'image/jpeg',
  preserveAlpha: true
};

/** 重编码会改变语义、直接放行的图片类型 */
const SKIP_MIME_TYPES = new Set(['image/svg+xml', 'image/svg', 'image/gif']);

const MIME_EXTENSIONS: Record<CompressImageMimeType, string> = {
  'image/jpeg': 'jpg',
  'image/png': 'png'
};

/**
 * 该文件是否可以走 canvas 重编码
 * @param file 待上传文件
 * @return 是否允许压缩
 */
export function isCompressibleImage(file: Pick<File, 'type'>): boolean {
  const mimeType = String(file?.type ?? '').toLowerCase();
  return mimeType.startsWith('image/') && !SKIP_MIME_TYPES.has(mimeType);
}

/**
 * 等比缩放到长边不超过 maxEdge；小图不放大
 * @param width 原图宽
 * @param height 原图高
 * @param maxEdge 长边上限
 * @return 缩放后的宽高；非法尺寸返回 0
 */
export function fitWithin(width: number, height: number, maxEdge: number): { width: number; height: number } {
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
    return { width: 0, height: 0 };
  }
  if (!Number.isFinite(maxEdge) || maxEdge <= 0) {
    return { width: Math.round(width), height: Math.round(height) };
  }
  const scale = Math.min(1, maxEdge / Math.max(width, height));
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale))
  };
}

/**
 * 把扩展名替换成输出格式对应的扩展名，保留文件名主体
 * @param name 原文件名
 * @param mimeType 输出 MIME
 * @return 新文件名
 */
export function withFileExtension(name: string, mimeType: CompressImageMimeType): string {
  const baseName = String(name ?? '').replace(/\.[^./\\]*$/, '') || 'image';
  return `${baseName}.${MIME_EXTENSIONS[mimeType]}`;
}

/**
 * 按允许的扩展名选择输出格式：允许 jpg/jpeg 时优先 JPEG，否则保持 PNG
 * @param fileTypes 组件允许的扩展名列表，如 ['png', 'jpg']
 * @return 输出 MIME
 */
export function resolveOutputMimeType(fileTypes: unknown[]): CompressImageMimeType {
  const normalized = (fileTypes ?? []).map((type) => String(type).toLowerCase().replace(/^\./, ''));
  if (!normalized.length) {
    return 'image/jpeg';
  }
  return normalized.some((type) => type === 'jpg' || type === 'jpeg') ? 'image/jpeg' : 'image/png';
}

/**
 * 把压缩结果同步回上传组件持有的文件对象
 * TDesign 的 UploadFile 在选中文件时缓存了 name/size/type，只替换 raw 会让列表显示和后续校验读到旧值
 * @param target 上传组件内部文件对象
 * @param source 压缩后的文件
 */
export function syncUploadFile(target: { raw?: File; name?: string; size?: number; type?: string }, source: File): void {
  target.raw = source;
  target.name = source.name;
  target.size = source.size;
  target.type = source.type;
}

/**
 * 压缩图片文件；无法压缩或压缩无收益时返回原文件
 * @param file 原始图片文件
 * @param options 压缩参数
 * @return 压缩后的文件；失败时返回原文件
 */
export async function compressImage(file: File, options: CompressImageOptions = {}): Promise<File> {
  if (!isCompressibleImage(file)) {
    return file;
  }

  const config: Required<CompressImageOptions> = { ...DEFAULT_OPTIONS, ...options };
  if (config.skipBelowKB > 0 && file.size <= config.skipBelowKB * 1024) {
    return file;
  }

  let release: (() => void) | undefined;
  try {
    const loaded = await loadImage(file);
    release = loaded.release;
    const rendered = await renderImage(loaded.image, config);
    if (!rendered) {
      return file;
    }

    // 同格式且压缩后反而更大时保留原图；跨格式转换（如头像 PNG -> JPEG）以格式统一为先
    const formatChanged = String(file.type).toLowerCase() !== rendered.mimeType;
    if (!formatChanged && rendered.blob.size >= file.size) {
      return file;
    }

    return new File([rendered.blob], withFileExtension(file.name, rendered.mimeType), {
      type: rendered.mimeType,
      lastModified: file.lastModified
    });
  } catch (error) {
    console.warn('[image] 图片压缩失败，使用原图上传', error);
    return file;
  } finally {
    release?.();
  }
}

/**
 * 把图片绘制到画布并按目标格式编码
 * @param image 已解码的图片
 * @param config 压缩配置
 * @return 编码结果和实际输出格式；画布不可用时返回 null
 */
async function renderImage(
  image: HTMLImageElement,
  config: Required<CompressImageOptions>
): Promise<{ blob: Blob; mimeType: CompressImageMimeType } | null> {
  const sourceWidth = image.naturalWidth || image.width;
  const sourceHeight = image.naturalHeight || image.height;
  const scaled = fitWithin(sourceWidth, sourceHeight, config.maxEdge);
  if (!scaled.width || !scaled.height) {
    return null;
  }

  const canvas = document.createElement('canvas');
  canvas.width = scaled.width;
  canvas.height = scaled.height;
  const context = canvas.getContext('2d');
  if (!context) {
    return null;
  }
  context.drawImage(image, 0, 0, scaled.width, scaled.height);

  // 透明图转 JPEG 会把透明像素画成黑色：默认保留 PNG，显式关闭 preserveAlpha 时补白底
  let mimeType = config.mimeType;
  if (config.preserveAlpha && mimeType === 'image/jpeg' && hasTransparency(canvas)) {
    mimeType = 'image/png';
  }
  if (mimeType === 'image/jpeg') {
    context.globalCompositeOperation = 'destination-over';
    context.fillStyle = JPEG_BACKGROUND;
    context.fillRect(0, 0, scaled.width, scaled.height);
    context.globalCompositeOperation = 'source-over';
  }

  const targetBytes = config.maxSizeKB > 0 ? config.maxSizeKB * 1024 : Number.POSITIVE_INFINITY;
  const blob =
    mimeType === 'image/jpeg'
      ? await encodeJpegToTargetSize(canvas, config.quality, MIN_JPEG_QUALITY, targetBytes)
      : await canvasToBlob(canvas, mimeType);
  return blob ? { blob, mimeType } : null;
}

/**
 * 用 object URL 解码图片；drawImage 时会按浏览器默认行为应用 EXIF 方向
 * @param file 图片文件
 * @return 图片元素和释放 object URL 的回调
 */
function loadImage(file: File): Promise<{ image: HTMLImageElement; release: () => void }> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const release = () => URL.revokeObjectURL(url);
    const image = new Image();
    image.onload = () => resolve({ image, release });
    image.onerror = () => {
      release();
      reject(new Error('图片解码失败'));
    };
    image.decoding = 'async';
    image.src = url;
  });
}

/**
 * 用 64px 缩略图判断是否存在透明像素，避免对大图做全量 getImageData
 * @param canvas 已绘制原图的画布
 * @return 是否存在透明像素
 */
function hasTransparency(canvas: HTMLCanvasElement): boolean {
  const sampleSize = 64;
  const scale = Math.min(1, sampleSize / Math.max(canvas.width, canvas.height));
  const sample = document.createElement('canvas');
  sample.width = Math.max(1, Math.round(canvas.width * scale));
  sample.height = Math.max(1, Math.round(canvas.height * scale));
  const context = sample.getContext('2d');
  if (!context) {
    return false;
  }
  context.drawImage(canvas, 0, 0, sample.width, sample.height);
  const data = context.getImageData(0, 0, sample.width, sample.height).data;
  for (let index = 3; index < data.length; index += 4) {
    if (data[index] < 255) {
      return true;
    }
  }
  return false;
}

/**
 * 把画布编码成 Blob
 * @param canvas 目标画布
 * @param mimeType 输出格式
 * @param quality 编码质量（仅 JPEG 生效）
 * @return 编码后的 Blob
 */
function canvasToBlob(canvas: HTMLCanvasElement, mimeType: CompressImageMimeType, quality?: number): Promise<Blob | null> {
  return new Promise((resolve) => canvas.toBlob(resolve, mimeType, quality));
}

/**
 * 在质量区间内二分搜索，返回不超过 targetBytes 的最高质量；都超标时返回体积最小的结果
 * @param canvas 已绘制原图的画布
 * @param quality 初始质量
 * @param minQuality 质量下限
 * @param targetBytes 目标字节数
 * @return 满足目标体积的 Blob；无法满足时返回最小体积结果
 */
async function encodeJpegToTargetSize(canvas: HTMLCanvasElement, quality: number, minQuality: number, targetBytes: number): Promise<Blob | null> {
  const initial = await canvasToBlob(canvas, 'image/jpeg', quality);
  if (!initial || initial.size <= targetBytes) {
    return initial;
  }

  let best: Blob | null = null;
  let smallest = initial;
  let lower = Math.max(0.1, Math.min(minQuality, quality));
  let upper = quality;

  // toBlob 的质量粒度约 0.01，6 次二分足够收敛
  for (let index = 0; index < 6 && upper - lower > 0.02; index++) {
    const middle = (lower + upper) / 2;
    const blob = await canvasToBlob(canvas, 'image/jpeg', middle);
    if (!blob) {
      break;
    }
    if (blob.size <= targetBytes) {
      best = blob;
      lower = middle;
    } else {
      upper = middle;
    }
    if (blob.size < smallest.size) {
      smallest = blob;
    }
  }

  return best ?? smallest;
}
