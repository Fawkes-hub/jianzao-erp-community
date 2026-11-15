# 部署（Docker Compose）

四个容器：`nginx`（静态资源 + 反向代理）、`erp-app`（Spring Boot）、`mysql` 8.0、`redis` 7.2。
只有 Nginx 映射宿主机端口，MySQL / Redis 仅在内网可达。

本目录只包含 Compose 定义、Dockerfile、Nginx 配置与初始化 SQL，**不含构建脚本**——构建产物需要你自己生成（见下）。

## 1. 准备构建产物

Compose 消费两个本地目录，二者都不入库：

**后端 JAR → `deploy/app/erp.jar`**

```bash
# 底座 → 业务模块 → 可执行包，顺序不能颠倒
cd jianzao-backend && mvn clean install -DskipTests
cd ../erp-modules && mvn clean install -DskipTests
cd ../jianzao-backend/jianzao-admin && mvn -Pprod package -DskipTests
mkdir -p ../../deploy/app
cp target/jianzao-admin.jar ../../deploy/app/erp.jar
```

**前端静态文件 → `deploy/frontend-dist/`**

```bash
cd jianzao-erp
pnpm install --frozen-lockfile
pnpm build:prod
rm -rf ../deploy/frontend-dist
cp -r dist ../deploy/frontend-dist
```

## 2. 配置环境变量

```bash
cd deploy
cp .env.example .env
chmod 600 .env
```

`.env` 里四项必须改，且不要复用示例值：

| 变量 | 说明 |
|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `MYSQL_PASSWORD` | 应用账号密码 |
| `REDIS_PASSWORD` | Redis 密码 |
| `JWT_SECRET` | Sa-Token 签名密钥，`openssl rand -hex 32` 生成 |

公网部署还要设 `SECURITY_CORS_ALLOWED_ORIGINS`（逗号分隔），否则浏览器带 `Origin` 的 POST 会被 CORS 过滤器判为非法跨域并返回 403。

## 3. 启动

```bash
docker compose up -d
docker compose ps          # 等 mysql / redis / erp-app 全部 healthy
```

首次启动时 MySQL 会按文件名顺序执行 `mysql/init/` 下的脚本（01 系统表 → 04 ERP 表结构 → 05 菜单与种子 → 06 图标）。
访问 `http://<服务器IP>:${HTTP_PORT:-80}`，用 `admin` / `admin123` 登录（与上游 RuoYi 默认一致），登录后立即改密。

## 常用命令

```bash
docker compose logs -f --tail=200 erp-app
docker compose restart erp-app
docker compose down            # 保留数据卷
docker compose down -v         # 连数据一起删除，谨慎
```

## 数据与备份

- 数据库：Docker 卷 `mysql-data`
- Redis：Docker 卷 `redis-data`
- 应用数据（上传文件、临时目录）：`./data`
- 备份：`docker compose exec mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" | gzip > backup-$(date +%F).sql.gz`

恢复前先停 `erp-app`，恢复后检查登录、单据、库存与打印。

## HTTPS

`nginx/conf.d/erp.conf` 默认只监听 80。启用 HTTPS 时取消该文件底部的 443 server 块注释，把证书放到 `nginx/certs/`（`fullchain.pem` / `privkey.pem`），再 `docker compose restart nginx`。

## 端口与网络

| 服务 | 宿主机 | 容器内 |
|---|---|---|
| nginx | `${HTTP_PORT:-80}` / `${HTTPS_PORT:-443}` | 80 / 443 |
| mysql | `127.0.0.1:${MYSQL_PORT:-13306}` | 3306 |
| redis | 不映射 | 6379 |
| erp-app | 不映射 | 8080 |

MySQL / Redis 绑定在回环地址或完全不映射，不要把 3306 / 6379 暴露到公网。
