# 发布前门禁 / Preflight

> ⚠️ 本文件是**项目方内部检查表**，不是公开文档 —— **发布前必须删除**。
> 本仓库当前仅含发布配套文件，**未导入任何源码、未 `git init`、未提交、未推送**。

## 当前状态

| 项 | 状态 |
|---|---|
| 目录 | `/Users/fawkes/Documents/ERP/jianzao-erp-community/`（与主仓 `erp/` 平级，独立） |
| 源码 | ✅ 已导入并裁剪（1543 个跟踪文件，约 20 MB） |
| Git 仓库 | ✅ 已初始化，10 个策展提交，单一公开身份，`git fsck` 干净 |
| 公开仓库 | ❌ 未创建 |
| 推送 | ❌ 无（**按约定不自行建远端、不推送**） |
| 后端编译 | ✅ `mvn -o -f jianzao-backend/pom.xml -pl jianzao-admin -am compile -DskipTests` → BUILD SUCCESS（26 模块） |
| 前端构建 | ✅ `pnpm install --frozen-lockfile` + `pnpm build:dev` → EXIT=0（仅 chunk 体积警告） |
| 敏感串 | ✅ 全仓 grep 归零（AppID / VPS / 域名 / clientId / 客户名 / 公司邮箱 / 内部称呼 / 旧品牌） |
| 私有资产 | ✅ 无 `.env*`（仅 `.env.example`）、无 `dist/`、无二进制图片素材、无原 `.git` 对象 |

主仓 `erp/` 全程只读，未修改任何文件。

**注意**：本文件是内部检查表，**发布前必须删除**；`PREFLIGHT.md` 自身已被 git 跟踪，删除时需 `git rm`。

## 已起草的文件

| 文件 | 用途 | 状态 |
|---|---|---|
| `LICENSE` | AGPL-3.0 官方全文（gnu.org 原文，661 行，SHA256 `0d96a4ff…b0`） | 可直接用 |
| `README.md` | 公开首页：定位、技术栈、UI 亮点、功能范围、快速开始 | 框架，需填仓库 URL |
| `THIRD_PARTY_NOTICES.md` | 上游 MIT 归因 + 授权未明素材清单 | 需复核 |
| `TRADEMARK.md` | 名称/logo 不授权、fork 改名规则 | 草案，需律师复核 |
| `COMMERCIAL.md` | 商业双许可说明与联系入口 | 草案，需填邮箱 |
| `CLA.md` | 个人 ICLA（可再授权 / 可用于专有许可） | 草案，需律师复核 |
| `CCLA.md` | 企业贡献者协议 | 草案，需律师复核 |
| `CONTRIBUTING.md` | 贡献流程 + CLA 前置 | 需启用签署工具 |
| `docs/ui-migration.md` | 公开 TDesign 迁移实践笔记（1–2 页目标） | 框架，需核对行号数字 |
| `docs/history.md` | 策展历史说明 | 框架，需填 hash/日期 |
| `.gitignore` | 阻断 `.env*` / `dist/` / 构建产物 | 可用 |

## 硬性阻断项（逐项做完才能设为公开）

### A. 法务

- [ ] 律师复核：`LICENSE` 与上游 MIT 组合、`TRADEMARK.md` 与 AGPL 兼容性、`CLA.md` / `CCLA.md` 正文、商业双许可条款、衍生作品边界
- [ ] 商业授权专用邮箱建立并填入 `COMMERCIAL.md`（**不要复用个人邮箱**）
- [ ] CLA 留存邮箱填入 `CONTRIBUTING.md`
- [ ] `CLA.md` / `CCLA.md` / `TRADEMARK.md` / `COMMERCIAL.md` 末段「起草说明」**全部删除**
- [ ] 决定并写好商业授权协议正文（条款、费用、期限、责任限制、终止）
- [ ] 考虑注册「简造」商标（第 35/42 类）以增强 `TRADEMARK.md` 可执行性
- [ ] 若接受境外贡献者，补 CLA/CCLA 英文版并明确以何者为准

### B. CLA 工具（顺序硬要求：**先上线，后公开**）

- [ ] 选定并接入 `cla-assistant.io` 或等价 GitHub Action
- [ ] PR 上自动校验签署状态；配置 CCLA 企业白名单
- [ ] 签署记录留存（姓名/邮箱/GitHub ID/日期/commit 范围）
- [ ] `CONTRIBUTING.md` 第 1 节收敛为实际采用的一种签署方式
- [ ] 验证：用一个测试 PR 跑通「未签 → 阻断 → 签署 → 放行」

### C. 品牌与名称

- [ ] 确定公开仓库 slug（建议 `jianzao-erp-community`）与组织/账号
- [ ] `README.md` 中 `ORG_PLACEHOLDER` 占位符替换
- [ ] `jianzao-erp/package.json:21` repository URL 换新公开仓
- [ ] 确定社区版与商业版 UI 差异方案（配色 / 标识）
- [ ] 导入时逐文件中和品牌字面量（约 14 处 / 10 文件，含 i18n、logo SVG 的 `aria-label`、`dashboard/index.vue:361` 裸串）
- [ ] 验收：对导入后的 `src/`、`dist/`、`.env*` 全量 grep 品牌字面量与私有串，归零（**注意**：README/TRADEMARK/COMMERCIAL 中「简造」作为项目名是允许的，需区分）

### D. 密钥与私有资产（最高优先级）

- [ ] **三个 tracked `.env*` 的 RSA 私钥 + clientId 删除并轮换**：`.env.development:26,28,31`、`.env.localwin:28,30,33`、`.env.production:28,30,33`；若线上仍用该密钥对，视为已泄露
- [ ] **两个 ignored `.env*.local` 物理删除**：`.env.localwin.local`（客户公司真名）、`.env.development.local`（内网 `orb.local`）—— 按目录复制会带上
- [ ] `dist/` 整个删除或改完源码重建（353 文件里品牌串已编译进 10+ 个 JS）
- [ ] 真实 AppID、生产 VPS 地址与 SSH 配置、商业域名、OSS 端点、固定 JWT key、clientId 全部清洗（具体值见仓库外确认清单）
- [ ] `DOC/` 全部 115 文件、`desktop/`、`build/`、`erp-miniapp/`、`.idea/`、`.zcode/`、`.openidea/`、`.workbuddy/`、`.DS_Store` —— **一律不带入**
- [ ] 原始 `.git` 整体不带入（含 dangling commit 里的真名+公司邮箱、74 个 unreachable 对象、reflog、tag、worktrees）

### E. 授权未明的素材（见 `THIRD_PARTY_NOTICES.md` §4）

- [ ] `public/tac/*` 天爱验证码 SDK 替换或取得授权
- [ ] `login-background.jpg` / `profile.jpg` / 401/404 插画替换或核实授权
- [ ] `src/assets/icons/svg/` 第三方品牌字形核实
- [ ] `deploy/mysql/init/01-ruoyi-system.sql` 补 MIT 头
- [ ] `jianzao-backend/LICENSE`、`jianzao-erp/LICENSE`、`VENDOR.md` 原文保留
- [ ] `package.json:7` license 改为 `AGPL-3.0`

### F. 源码裁剪（按 `开源模块确认清单.md` 第三、四节）

- [ ] 删 `erp-production` 整模块 + 5 处 Maven 引用 + 6 个 `.flattened-pom.xml`
- [ ] 删工作台 / 盘点 / 备份三个付费功能
- [ ] 删 3030 生产单、3300 委外两组菜单 + 相关 SQL / 迁移 / 补丁 / manifest 哈希
- [ ] 修 `2026-11-03-backfill.sql:135,137` 的 `SELECT biz_production*` 分支
- [ ] 加一份针对已有安装的清理迁移（`sys_menu` / `sys_role_menu` / `sys_tenant_package`）
- [ ] 清理 `PRODUCTION` 字符串（**保留 `PRODUCTION_CONSUMABLE`**，它是「其他出库」成本分类）
- [ ] 删 SaaS 账户层 / 试用 / 套餐 / 游客租户 999999 / 微信扫码登录绑定
- [ ] `mvn -pl jianzao-backend/jianzao-admin -am compile` + 单测（`-Pdev`，JDK 17）
- [ ] `pnpm build`；核心单据回归（采购/销售/退货/其他出库/期初/对账单/打印/订单 M1）

### G. 策展历史

- [ ] 在新仓库按 8 个里程碑重建（**新建空仓库，不 clone 主仓**）
- [ ] 每次提交只放当时公开子集的干净快照；作者统一为公开身份
- [ ] `docs/history.md` 填真实 hash / 日期；README 链接本文并注明「按里程碑重建」
- [ ] 确认新 `.git` 无原仓库对象（`git fsck --unreachable` 干净、无 tag、无 reflog 残留、`.git/config` 无私有 remote）

### H. 最后

- [ ] 删除本文件 `PREFLIGHT.md`
- [ ] `git ls-files` 白名单逐项核对，确认无遗漏私有文件
- [ ] 复核 [开源模块确认清单.md](../开源模块确认清单.md) 所有勾选项
- [ ] **由项目发起人决定**是否 `git init` / commit / 创建远端 / 推送 —— 在此之前不执行

---

## 参考

- 完整边界与勾选表：`/Users/fawkes/Documents/ERP/开源模块确认清单.md`
- 主仓：`/Users/fawkes/Documents/ERP/erp/`（只读，未改动）
