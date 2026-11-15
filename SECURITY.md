# 安全策略 / Security Policy

## 报告漏洞

**请勿开公开 issue。** 请通过 GitHub 仓库的 **Security → Advisories → Report a vulnerability** 私下报告，或直接联系仓库维护者。

报告时请尽量包含：

- 受影响版本或 commit
- 复现步骤与前置条件（是否需要登录、角色、租户）
- 影响范围（越权、注入、敏感信息泄露、拒绝服务等）
- 可选：修复建议或 PoC

我们会在确认后尽快回复，并在修复发布后致谢（如你希望具名）。

## 支持范围

安全修复只针对 `main` 分支的最新提交。社区版不提供 SLA。

## 部署方须知

本项目是**应用骨架**，默认配置面向本地开发，直接暴露到公网前请至少完成：

1. 修改 `deploy/.env` 中全部示例密码，`JWT_SECRET` 用 `openssl rand -hex 32` 生成；
2. 启用 HTTPS，不要把 MySQL（3306）/ Redis（6379）映射到公网；
3. 配置 `SECURITY_CORS_ALLOWED_ORIGINS` 为实际访问来源，不要留 `*`；
4. 按需启用接口加密：见 `application.yml` 中 `api-decrypt` 的注释（前后端必须使用同一密钥对）；
5. 如需验证码，先自备已授权的滑块 SDK 放入 `jianzao-erp/public/tac/`，再设 `captcha.enable: true`；
6. 上线后立即修改 `admin` 默认密码，删除或禁用不需要的演示账号与租户。

## 不在范围内

- 依赖包自身的漏洞（请同时反馈给上游）
- 未按上述部署须知配置导致的弱口令、明文传输、越权访问
- 上游 RuoYi-Vue-Plus / plus-ui 的既有问题（请反馈给上游）
