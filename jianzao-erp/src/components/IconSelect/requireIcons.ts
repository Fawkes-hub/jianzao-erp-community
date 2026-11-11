import { manifest } from 'tdesign-icons-vue-next';

/**
 * TDesign 官方图标全集名称。
 * 图标名即 `<t-icon name="..." />` 的 name，供菜单图标选择器搜索与渲染。
 */
const icons: string[] = manifest.map((item) => item.stem);

export default icons;
