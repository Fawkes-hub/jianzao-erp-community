# Vendored Upstream Code

本项目把两个上游项目作为 **vendored 代码** 并入根仓库，保证文档、业务代码、底座和部署脚本在同一版本历史中。

## 1. jianzao-backend（原 RuoYi-Vue-Plus 后端底座）

- 上游：https://github.com/dromara/RuoYi-Vue-Plus
- 分支：`5.X`
- 拉取提交：`815087e733eddd641dce8ee3bdb72f21e1b08e28`
- 目录名与模块：已从 `RuoYi-Vue-Plus` 全量重命名为 `jianzao-backend`；底层模块（`jianzao-common-*`、`jianzao-modules`、`jianzao-extend`、`jianzao-admin`）及业务模块（`jianzao-erp-*`）已彻底收敛为简造架构体系。
- 许可证：MIT（见 `jianzao-backend/LICENSE`）
- 本地定制：
  - `deploy/` 服务端部署与配置
  - `erp-modules/` ERP 业务模块（6 个 Maven 子模块：master、stock、trade、production、fin、print）
  - 配置键、租户/审计字段对齐
  - 放宽菜单权限标识校验（`RegexConstants.PERMISSION_STRING`）：允许 `-` 和 3 段以上，兼容 `erp:other-in:list`、`erp:finance:cost-report:export` 等业务权限标识
- 更新策略：
  - 锁定版本；上游升级时手动 diff/cherry-pick，不直接覆盖业务模块。
  - 修改底座核心必须记录原因与提交号。

## 2. jianzao-erp（前端工程）

- 原上游：https://github.com/CrazyLionCat/plus-ui（基于 5.X，已彻底剥离解耦，独立维护）
  - 注：该仓库当前默认分支为 `6.X-Vue`；本项目锁定的是 **5.X** 时期的快照，不再跟随上游分支
- 本项目仓库：https://github.com/Fawkes-hub/jianzao-erp-community.git （子目录 `jianzao-erp`）
- 许可证：MIT（见 `jianzao-erp/LICENSE`）
- 本地定制：
  - 工程与目录名称统一定为 `jianzao-erp`；`package.json` 包名 `jianzao-erp` / `1.0.0`
  - 品牌名称“简造ERP社区版”（全称：简造ERP社区版）
  - 组件库从 Element Plus 全量迁至 **TDesign Vue Next 1.20.10**，已删除 `element-plus` / `@element-plus/icons-vue` 依赖及全部源码引用
  - 关闭前端应用层 RSA 加密（`VITE_APP_ENCRYPT=false`，依赖 HTTPS）
  - `src/views/erp/` ERP 业务页面
- 更新策略：**组件库已整体替换、代码已完全剥离，独立自主演进，不再与上游同步**。

## 3. 复制到部署目录的 SQL

- `deploy/mysql/init/01-ruoyi-system.sql`

以上来自 `jianzao-backend/script/sql/`，对应当前 vendored 提交；保留上游版权与 MIT 许可。
ERP v1 已裁剪任务调度与工作流，`ry_job.sql`、`ry_workflow.sql` 未复制。

以下为本项目自研：

- `deploy/mysql/init/04-erp-schema.sql`
- `deploy/mysql/init/05-erp-seed.sql`

## 4. 安全说明

- 上游示例密码/密钥（如 `root/root`、`ruoyi123`、示例 JWT/API 密钥）只是默认值，不是生产凭据。
- 生产凭据放在 `deploy/.env`，该文件已被 `.gitignore` 排除，不入库。
- 提交前请确认没有把真实密码、Token、证书私钥加入版本库。
