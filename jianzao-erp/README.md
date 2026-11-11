# 简造ERP社区版· 前端（jianzao-erp）

面向 2~3 人小型五金机加工厂的轻量开单记账系统前端工程。基于 Vue 3 + TypeScript + Vite + TDesign Vue Next + Pinia 构建。

## 技术栈

- **框架**：[Vue 3](https://v3.cn.vuejs.org/) (`<script setup>` + TypeScript)
- **构建工具**：[Vite](https://cn.vitejs.dev/)
- **UI 组件库**：[TDesign Vue Next](https://tdesign.tencent.com/vue-next/overview)
- **状态管理**：[Pinia](https://pinia.vuejs.org/)
- **路由管理**：[Vue Router 4](https://router.vuejs.org/)
- **样式与图标**：TDesign Icons + UnoCSS
- **数据报表与图表**：[ECharts](https://echarts.apache.org/)
- **表格增强**：vxe-table

## 本地开发与构建

```bash
# 1. 安装依赖
pnpm install

# 2. 启动本地开发服务（需后端配合启动）
pnpm dev

# 3. 生产环境构建
pnpm build:prod

# 4. Windows 离线包构建（Hash 路由模式）
pnpm build:localwin
```

## 目录结构说明

```
jianzao-erp/
├── public/              # 静态资源
├── src/
│   ├── api/             # 接口请求（按业务模块划分）
│   ├── assets/          # 静态图标与样式资源
│   ├── components/      # 通用与业务公共组件（含 erp 业务选择器等）
│   ├── layout/          # 布局架构与侧边导航
│   ├── router/          # 路由配置
│   ├── store/           # Pinia 状态管理
│   ├── utils/           # 通用工具函数
│   └── views/           # 页面视图（erp 业务模块、系统设置等）
├── vite/                # Vite 插件配置
├── package.json         # 项目依赖与构建脚本
└── vite.config.ts       # Vite 主配置
```
