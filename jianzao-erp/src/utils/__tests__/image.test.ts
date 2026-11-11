import { describe, expect, it } from 'vitest';
import { compressImage, fitWithin, isCompressibleImage, resolveOutputMimeType, syncUploadFile, withFileExtension } from '../image';

/**
 * 只覆盖压缩工具的纯函数部分：
 * canvas/Image 解码依赖浏览器环境，交给组件联调；这里守住尺寸、扩展名和格式选择的边界。
 */
describe('isCompressibleImage', () => {
  it('普通位图可以压缩', () => {
    expect(isCompressibleImage({ type: 'image/jpeg' })).toBe(true);
    expect(isCompressibleImage({ type: 'image/png' })).toBe(true);
    expect(isCompressibleImage({ type: 'image/bmp' })).toBe(true);
  });

  it('SVG/GIF 跳过重编码', () => {
    expect(isCompressibleImage({ type: 'image/svg+xml' })).toBe(false);
    expect(isCompressibleImage({ type: 'image/gif' })).toBe(false);
  });

  it('非图片或空类型跳过', () => {
    expect(isCompressibleImage({ type: 'application/pdf' })).toBe(false);
    expect(isCompressibleImage({ type: '' })).toBe(false);
  });
});

describe('fitWithin', () => {
  it('横图按长边等比缩小', () => {
    expect(fitWithin(4000, 3000, 1920)).toEqual({ width: 1920, height: 1440 });
  });

  it('竖图按长边等比缩小', () => {
    expect(fitWithin(3000, 4000, 1920)).toEqual({ width: 1440, height: 1920 });
  });

  it('小图不放大', () => {
    expect(fitWithin(100, 80, 1920)).toEqual({ width: 100, height: 80 });
  });

  it('maxEdge <= 0 表示不限制', () => {
    expect(fitWithin(1000, 500, 0)).toEqual({ width: 1000, height: 500 });
  });

  it('非法尺寸返回 0，交给调用方回退原图', () => {
    expect(fitWithin(0, 100, 1920)).toEqual({ width: 0, height: 0 });
    expect(fitWithin(Number.NaN, 100, 1920)).toEqual({ width: 0, height: 0 });
  });

  it('缩放后至少保留 1px', () => {
    expect(fitWithin(1000, 10, 20)).toEqual({ width: 20, height: 1 });
  });
});

describe('withFileExtension', () => {
  it('替换原扩展名并保留文件名主体', () => {
    expect(withFileExtension('photo.PNG', 'image/jpeg')).toBe('photo.jpg');
    expect(withFileExtension('a.b.c', 'image/jpeg')).toBe('a.b.jpg');
  });

  it('无扩展名时补扩展名', () => {
    expect(withFileExtension('photo', 'image/png')).toBe('photo.png');
    expect(withFileExtension('', 'image/jpeg')).toBe('image.jpg');
  });

  it('保留中文文件名', () => {
    expect(withFileExtension('产品图.png', 'image/jpeg')).toBe('产品图.jpg');
  });
});

describe('resolveOutputMimeType', () => {
  it('允许 jpg/jpeg 时优先 JPEG', () => {
    expect(resolveOutputMimeType(['png', 'jpg', 'jpeg'])).toBe('image/jpeg');
    expect(resolveOutputMimeType(['.JPG'])).toBe('image/jpeg');
  });

  it('只允许 png 时保持 PNG', () => {
    expect(resolveOutputMimeType(['png'])).toBe('image/png');
  });

  it('未限制类型时默认 JPEG', () => {
    expect(resolveOutputMimeType([])).toBe('image/jpeg');
  });
});

describe('syncUploadFile', () => {
  it('同步 raw/name/size/type，避免列表和后续校验读到旧值', () => {
    const target: { raw?: File; name?: string; size?: number; type?: string } = {
      name: 'old.png',
      size: 1024,
      type: 'image/png'
    };
    const source = new File(['x'], 'new.jpg', { type: 'image/jpeg' });

    syncUploadFile(target, source);

    expect(target.raw).toBe(source);
    expect(target.name).toBe('new.jpg');
    expect(target.size).toBe(source.size);
    expect(target.type).toBe('image/jpeg');
  });
});

describe('compressImage（不进入 canvas 的分支）', () => {
  it('SVG/GIF 原样返回', async () => {
    const svg = new File(['<svg/>'], 'icon.svg', { type: 'image/svg+xml' });
    const gif = new File(['GIF89a'], 'anim.gif', { type: 'image/gif' });

    await expect(compressImage(svg)).resolves.toBe(svg);
    await expect(compressImage(gif)).resolves.toBe(gif);
  });

  it('低于阈值的小图原样返回', async () => {
    const file = new File([new Uint8Array(1024)], 'small.jpg', { type: 'image/jpeg' });

    await expect(compressImage(file)).resolves.toBe(file);
  });
});
