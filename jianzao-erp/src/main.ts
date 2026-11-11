import { createApp } from 'vue';
// global css
// 注意：Vite 会按模块图重排 CSS，源码里的 import 顺序管不住 UnoCSS 与组件库的先后（实测 .t-row 仍排在 .mb-[10px] 之后），
// 因此工具类要压过 TDesign 的 margin 简写时必须写 !mb-[10px]，不要改这里的顺序。
import 'virtual:uno.css';
// TDesign 组件样式必须先于本仓 index.scss 引入，让 --td-* → --app-* 桥接与 .erp-* 覆盖能压过组件默认值
import 'tdesign-vue-next/es/style/index.css';
import '@/assets/styles/index.scss';

// App、router、store
import App from './App.vue';
import store from './store';
import router from './router';

// UI 组件库：Element Plus 保留至 T6 清理，TDesign 供已迁移页面使用
import TDesign, { LoadingDirective } from 'tdesign-vue-next';

// 自定义指令
import directive from './directive';

// 注册插件
import plugins from './plugins/index'; // plugins

// 高亮组件
// import 'highlight.js/styles/a11y-light.css';
import 'highlight.js/styles/atom-one-dark.css';
import 'highlight.js/lib/common';
import HighLight from '@highlightjs/vue-plugin';

// svg图标
import 'virtual:svg-icons-register';

// permission control
import './permission';

// 国际化
import i18n from '@/lang/index';

// vxeTable
import VXETable from 'vxe-table';
import 'vxe-table/lib/style.css';
VXETable.setConfig({
  zIndex: 999999
});

const app = createApp(App);

app.use(HighLight);
app.use(TDesign);
app.directive('loading', LoadingDirective);
app.use(router);
app.use(store);
app.use(i18n);
app.use(VXETable);
app.use(plugins);
// 自定义指令
directive(app);

app.mount('#app');
