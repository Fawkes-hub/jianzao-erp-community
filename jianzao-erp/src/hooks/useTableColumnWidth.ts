/**
 * t-table 列宽记忆
 *
 * 把用户拖拽后的列宽按「表标识」存进 localStorage，下次进页面时合并回列配置。
 * 写入时机是 t-table 的 `@column-resize-change`——它在拖拽结束的 mouseup 里触发一次
 * （源码 useColumnResize 的 onDragEnd），不是 mousemove 期间，单表一次写 200~400 字节；
 * 读取只在页面初始化时发生一次，之后走内存缓存，所以对渲染帧没有影响。
 */

/** 存储前缀；带 v1 版本号，列定义大改时改版本即可整体作废旧记忆 */
const STORAGE_PREFIX = 'erp:table-cols:v1:';

/** 已读过的列宽，避免 computed 每次重算都访问 localStorage */
const widthCache = new Map<string, Record<string, number>>();

/**
 * 读取某张表记忆的列宽（只读一次 localStorage，其余走缓存）
 * @param key 表标识，约定 `页面/表名`，例如 purchase/detail
 * @returns colKey -> 像素宽度
 */
export function readColumnWidths(key: string): Record<string, number> {
  const cached = widthCache.get(key);
  if (cached) return cached;
  let widths: Record<string, number> = {};
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + key);
    if (raw) widths = JSON.parse(raw) as Record<string, number>;
  } catch {
    // 记忆损坏或隐私模式禁用 storage：按「没有记忆」处理，不影响表格
    widths = {};
  }
  widthCache.set(key, widths);
  return widths;
}

/**
 * 把记忆的列宽合并进列配置（记忆值优先于默认 width）
 * @param key 表标识
 * @param columns 列配置数组；函数内就地改 width，便于静态数组与 computed 统一调用
 * @returns 传入的列配置数组
 */
export function mergeSavedWidths<T extends any[]>(key: string, columns: T): T {
  const widths = readColumnWidths(key);
  columns.forEach((col) => {
    const width = widths[col.colKey];
    if (col.colKey && width) col.width = width;
  });
  return columns;
}

/**
 * 保存拖拽后的列宽
 * @param key 表标识
 * @param columnsWidth t-table `@column-resize-change` 回调里的 columnsWidth（colKey -> 宽度）
 */
export function saveColumnWidths(key: string, columnsWidth: Record<string, number>): void {
  if (!columnsWidth || !Object.keys(columnsWidth).length) return;
  widthCache.set(key, columnsWidth);
  try {
    localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(columnsWidth));
  } catch {
    // 配额满或被禁用：当次拖拽仍生效，只是记不住
  }
}
