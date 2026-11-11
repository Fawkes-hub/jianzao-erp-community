# ERP 服务端部署（Docker Compose）

> 详细说明见 [DOC/15-部署与运维手册.md](../DOC/15-部署与运维手册.md)。

## 组成

- `nginx`：静态资源 + 反向代理（对外 80/443）
- `erp-app`：Spring Boot 应用（内部 8080）
- `mysql`：MySQL 8.0（内部 3306）
- `redis`：Redis 7.2（内部 6379）

只有 Nginx 映射宿主机端口；MySQL/Redis 不暴露公网。

## 首次部署

1. **构建后端 JAR**（需 JDK 17+，在 `deploy/` 目录执行；等价于仓库根目录的 `./deploy/build_backend.sh`）：
   ```bash
   ./build_backend.sh
   ```
   脚本会构建并复制到 `deploy/app/erp.jar`。
2. **构建前端并部署**（脚本会构建 + 同步到 `deploy/plus-ui-dist/` + 重启 nginx）：
   ```bash
   ./deploy_frontend.sh
   ```
3. **准备数据库脚本**：把 RuoYi-Vue-Plus 系统 SQL + ERP 表结构/种子数据放到 `deploy/mysql/init/`（MySQL 首次启动时按文件名顺序执行）。
4. **配置环境**：
   ```bash
   cp .env.example .env
   vi .env          # 改密码、端口、域名
   chmod 600 .env
   ```
5. **一键安装**：
   ```bash
   ./install.sh
   ```
6. 浏览器访问 `http://<服务器IP>:<HTTP_PORT>`，用 `admin` 和初始密码登录，立即改密。

## 常用命令

```bash
docker compose ps
docker compose logs -f --tail=200 erp-app
docker compose restart erp-app
./deploy_frontend.sh          # 构建前端并部署到 nginx
./backup.sh
./restore.sh ./backup/20261031-020000
./upgrade.sh 1.1.0
```

## 远程发版（release.sh）

本地构建 → rsync 产物与部署定义 → 远端 `upgrade.sh` 升级 → 探活。

```bash
./deploy/release.sh --list                          # 查看可用环境
./deploy/release.sh --env vps                       # 交互式：选范围 → 确认 → 构建发版
./deploy/release.sh --env vps --scope frontend -y   # 只发前端
./deploy/release.sh --env vps --skip-build -y       # 复用已有 jar/dist，快速推送
./deploy/release.sh --env vps --skip-build --jar ./out/erp.jar --dist ./out/dist -y
./deploy/release.sh --env vps --dry-run             # 只打印命令，不做改动
```

行为说明：

- **部署定义每次同步**：`docker-compose.yml`、`Dockerfile.app`、`upgrade.sh`、`backup.sh`、`restore.sh`、`nginx/conf.d/`、`mysql/patches/`。新增 SQL 迁移后不会再出现远端 `patch not found`；但仍需把新补丁追加到 `upgrade.sh` 的 `UPGRADE_PATCHES`。
- **覆盖前留档**：旧的 `plus-ui-dist` 与部署定义会打成 `backup/ui-<时间>.tgz` / `backup/deploy-<时间>.tgz`，保留 30 天。
- **jar 按 sha256 跳过**：本地 jar 与远端一致时不重复上传 142MB。
- **nginx 按需 reload**：静态文件由 nginx 直接读盘；只有 `nginx/conf.d/` 内容变化才校验并 reload，避免每次发版约 5 秒的 502。
- **镜像 tag 唯一**：`--version` 指定的 tag 若已存在会直接拒绝，防止覆盖 `upgrade.sh` 回滚用的旧镜像。
- **探活**：后端用容器内 actuator 校验，入口再用 `HTTP_CHECK` 检查 HTTP 200。

环境文件：`deploy/environments/<name>.conf`（不入库，参考 `vps.conf.example`）。

## 数据与备份

- 上传文件：`./data/uploads`
- 数据库：Docker 卷 `mysql-data`
- 备份：`./backup/<时间戳>/db.sql.gz` + `uploads.tar.gz`，默认保留 30 天
- 恢复前请先备份当前数据；恢复后检查登录、单据、库存、打印

## 多客户

同一服务器跑多个客户时：

- 每个客户复制一份 `deploy/` 到独立目录；
- `.env` 中设置不同的 `COMPOSE_PROJECT_NAME`、`HTTP_PORT`、`MYSQL_DATABASE`、密码；
- 用不同域名/端口分流；数据卷/容器互不干扰。

## 本地 Windows 单机包

客户电脑免安装使用（双击 ERP.exe，内嵌 MySQL/Redis/JRE）见
[local-win/README.md](local-win/README.md)，由 GitHub Actions `build-win-package` 工作流构建。
