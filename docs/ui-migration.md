# 从 Element Plus 迁移到 TDesign：实践笔记

简造ERP社区版的前端由 plus-ui（Element Plus）全量迁移到 **TDesign Vue Next 1.20**，涉及 75 个视图、2,800+ 处组件标签。本文记录可复用的做法与踩过的坑。

## 1. 先说结论：这是一次「组件层」迁移

组件层已彻底迁移：`package.json` / `pnpm-lock.yaml` 无 `element-plus`，源码无 `<el-*>` 模板标签。

但视觉层是**混合体系**：`src/assets/styles/variables.module.scss` 保留了从 Element Plus `base.css` 的 `:root` 演化而来的 `--el-*` 变量，作为本仓自有的设计 token 兼容层（未改名为 `--td-*`）。所以准确定位是：

> TDesign 组件 + 库解耦的自研 token / 暗色体系

这样做的好处是：迁移期间视觉不跳变、暗色模式与业务卡片样式可继续沿用；代价是仓库里仍能看到 `--el-*` 命名，读代码时别误以为没迁完。

## 2. 自动导入与 CSS 顺序

`vite/plugins/components.ts` 用 `unplugin-vue-components` 的 `TDesignResolver({ library: 'vue-next' })` 全站自动导入，`dts` 生成到 `src/types/components.d.ts`，页面里无需逐个 import。

`src/main.ts` 里 CSS 引入顺序是硬约束：

```
tdesign-vue-next/es/style/index.css   ← 先
@/assets/styles/index.scss            ← 后（--td-* → --app-* 桥接、.erp-* 覆盖压过组件默认值）
virtual:uno.css
```

Vite 会按模块图重排 CSS，所以工具类要压过 TDesign 的 margin 简写时，得写成 `!mb-[10px]` 这类带 `!` 的形式，光靠 import 顺序不一定够。

UnoCSS 侧未引入任何 Web 字体（`presetWebFonts({ fonts: {} })`），全站用系统字体栈，没有字体授权负担。

## 3. 设计 token

`src/assets/styles/variables.module.scss`（约 480 行）分层组织：

| 组 | 用途 |
|---|---|
| `--app-radius-*` / `--app-shadow-*` | 圆角刻度、三级柔和阴影 |
| `--app-space-1..8` | 8 档间距，业务页唯一留白来源 |
| `--app-font-*` / `--app-text-*` | 字号阶梯（正文 ≥14px）、WCAG-AA 功能色（实测过白底对比度） |
| `--app-focus-ring` | 统一焦点环 |
| `--app-surface-hero-*` / `--app-chart-*` | 品牌渐变、四色图表调色板 |
| `--td-*` ↔ `--app-*` 桥接 | 在 `erp-surface.scss` 中完成 |

`html.dark` 下有完整覆盖（约 300 行），是真暗色而非反色滤镜，含 vxe-table 的暗色主题。文件末尾用 `:export` 把 SCSS 变量导出给 TS（`Sidebar/Logo.vue` 用得上）。

**特异性坑**：TDesign 的组件变量定义在 `:root (0,1,0)`，覆盖时必须用 `html:root (0,1,1)`；裸 `html (0,0,1)` 会被 `--td-brand-color` 抢回去。这个在 `erp-surface.scss` 开头有注释记录。

## 4. 语义 class 库

`src/assets/styles/erp-surface.scss` 定义了一套业务页可复用的 class：`.erp-surface`、`.erp-kpi`（KPI 卡、品牌渐变、暖色警示、趋势条）、`.erp-panel` / `.erp-panel-title`、`.erp-filter-card` / `.erp-filter-row` / `.erp-filter-actions`、`.erp-chart-card`、`.query-w-sm/md/lg`。

值得单独说的两处修复：

- TDesign 的 `t-form__controls` 默认 `flex: 0 1 auto`，会压掉固定宽度；查询区需要显式处理。
- TDesign 对表单项的 `--last` 判定会让校验提示压行，需针对性调整行距（全局收到 20px）。

列表统一用 `t-table resizable`，列宽可拖拽 —— 注意 **`resizable` 默认是 `false`**，所有列表/明细/详情表都要显式开启。

## 5. 组件映射里最容易翻车的几处

| 原 Element Plus | TDesign | 坑 |
|---|---|---|
| `:span="6/4/3/9"` 栅格 | `t-row/t-col` | **TDesign 是 12 栅格，不是 24**。直接沿用 EP 的 span 会让首个 `t-row` 合计 200%、字段挤成多行（`t-col-6` = `flex:0 0 50%`） |
| `<el-option>` 循环 | `t-select` 的 `{ label, value }[]` | 数据结构变了，不是模板标签替换 |
| `toggleRowSelection` | 受控 `selectedRowKeys` | 初始勾选要自己把已授权项写进受控数组 |
| `ElMessageBox.prompt` | 页内 `t-dialog` | 取消/关闭返回 `undefined`，必须显式中止后续动作 |
| `el-dropdown` 插槽 | `t-dropdown` | **插槽方向相反**：TDesign 默认插槽是触发器，`#dropdown` 才是面板；`@click` 回调整个 option 对象 |
| `el-card` | `t-card` | 只换外壳 ≠ 迁移完成；明细行里残留的 `el-date-picker` / `el-input-number` 会造成一屏两套设计语言 |

`TranslateIcon` 的图标视觉重量也调过：该图形笔画密度高，把 `stroke-width` 收到 `1.3` 才和顶栏其余 2px 图标一致。

## 6. 图标与无障碍

- `tdesign-icons-vue-next` 具名图标 + `vite-plugin-svg-icons-ng` 本地 SVG sprite 并存；
- 单图标调线宽要用 Vue 组件传 `strokeWidth`，雪碧图 `<use>` 方式改不动；
- 收尾门禁：无 emoji 当图标、可点元素三态、正文 ≥4.5:1、功能色不单靠颜色、数字列等宽右对齐、过渡 150–250ms、`prefers-reduced-motion`、375/768/1024/1440 四档不断版、硬编码 hex 与内联宽度归零、暗色逐页截图。

## 7. 打印与图表

`.erp-print-frame` 让打印预览 iframe 的边框/底色走主题变量，亮暗切换不突兀。ECharts 6 用在看板与缓存监控页，配合 `.erp-kpi` / `.erp-chart-card`。

## 8. 给准备做同类迁移的人

1. **先建 token 层再换组件** —— 否则每换一页都要重新调色，最后必然两套语言混搭。
2. **一批只做一类组件**，每批跑 375/768/1024/1440 四档截图。
3. **「换容器 ≠ 换组件」** 要当检查项：卡片外壳换了，里面的输入、日期、下拉、明细行都得逐个过。
4. **保留旧 token 命名不丢人**，把兼容层写进文档说明，比强行全量重命名风险低得多。
5. **暗色模式从一开始一起做**，后补几乎等于重做一遍。

---

本文仅描述技术做法。组件的版权与许可证见 [../THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)。
