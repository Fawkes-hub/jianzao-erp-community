# 贡献指南 / Contributing

欢迎参与简造ERP社区版。核心代码由项目发起人维护，社区贡献在 CLA 框架下接受。

---

## 1. 先签 CLA（硬性前置）

本项目采用 **AGPL-3.0 + 商业双许可**。为使项目方对含社区贡献的代码仍能单独授予商业许可，**任何外部代码贡献在合并前必须完成贡献者许可协议签署**：

| 你的身份 | 签什么 |
|---|---|
| 个人开发者、独立贡献 | [CLA.md](CLA.md)（ICLA） |
| 企业 / 组织员工，贡献属职务作品 | [CCLA.md](CCLA.md)，由雇主签署 |

**要点**：CLA **不转让你的版权**，你仍拥有自己的代码、仍可在别处使用；只是授予项目方一项可再授权、可用于专有/商业许可的永久许可。签署后项目方承诺继续以 AGPL-3.0 发布含你贡献的版本。

> ⚠️ **顺序要求**：CLA 签署流程必须在第一个外部 PR 合并前跑通 —— 事后补签很难。

**签署方式**：

1. 先提交 PR，在 PR 描述中注明「本人/本组织已阅读并同意 CLA」；
2. 维护者在 PR 中确认身份，并登记 GitHub ID / 时间 / commit 范围；
3. 企业贡献者请在 PR 中注明所属组织，由雇主签署 [CCLA.md](CCLA.md)，以便匹配企业白名单。

若后续接入 `cla-assistant.io` 或等价 GitHub Action，会在此处更新为自动校验。

---

## 2. 提交流程

1. 先开 issue 讨论（bug 复现步骤 / 功能动机 / 影响范围），避免白做；
2. fork → 建分支 → 小步提交，一个 PR 只解决一件事；
3. 提交信息用中文或英文均可，写清「改了什么、为什么」；
4. 自检：
   - 后端：`mvn -f jianzao-backend/pom.xml install -DskipTests` 后 `mvn -f erp-modules/pom.xml test`
   - 前端：`cd jianzao-erp && pnpm install --frozen-lockfile && pnpm build:prod`
   - 以上两条也是 CI 的检查项，PR 上必须全绿；
5. 发起 PR，完成 CLA 签署检查；
6. 项目方评审；可能需要补充测试或调整。

> 按项目约定，合并与提交由项目发起人控制；请勿直接在主分支提交。

## 3. 规范

- **后端**：Java 17、MyBatis-Plus、遵循现有 `org.erp.*` 包结构与命名；新业务表须带 `tenant_id` 与 `del_flag`；单号走 `SequenceService`；导出走公共 Excel；写操作加权限注解。
- **前端**：Vue 3 + TS + **TDesign Vue Next**；禁止引入 Element Plus 组件或新 `--el-*` 变量；样式优先用 `src/assets/styles/variables.module.scss` 的 `--app-*` token 与 `.erp-*` 语义 class；表单/表格遵循现有页面模式（见 [docs/ui-migration.md](docs/ui-migration.md)）。
- **不要提交**：`.env*`（密钥、品牌配置）、`dist/`、`node_modules/`、构建产物、IDE 配置、任何真实凭据、客户信息、内部文档。
- **安全**：发现漏洞请按 [SECURITY.md](SECURITY.md) 私下报告，不要开公开 issue。

## 4. 贡献范围

| 类型 | 是否接受 |
|---|---|
| Bug 修复、测试补充、文档改进、性能优化 | ✅ 欢迎 |
| 社区版范围内的功能完善（基础资料 / 进销存 / 财务 / 打印 / 订单） | ✅ 先讨论 |
| 生产/委外、盘点、看板、备份、小程序、SaaS 账户等**商业版功能** | ❌ 不属于社区版范围，不接收 |
| 大规模架构重构、换 UI 框架、全局格式化 | ❌ 先开 issue，通常不接受 |
| 纯依赖升级 | ✅ 可提，注明原因与影响 |

## 5. 行为准则

就事论事、尊重他人、不发布广告或无关内容。项目方有权关闭不合适的 issue / PR。

## 6. 许可证

提交贡献即表示你同意：你的贡献按 [CLA.md](CLA.md) / [CCLA.md](CCLA.md) 授权，并以 [LICENSE](LICENSE)（AGPL-3.0）向下游发布。第三方组件的声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
