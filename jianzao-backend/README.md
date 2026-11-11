# 简造 ERP · 后端服务 (jianzao-backend)

简造ERP社区版 / 简造 ERP 核心后端服务工程。基于 Spring Boot 3 与 Sa-Token 构建，集成多租户数据隔离、Redis 缓存、MyBatis-Plus-Join 连表查询与轻量级 SQL 拦截分析。

## 模块结构

- `jianzao-admin`：应用启动入口、认证鉴权与全局配置
- `jianzao-common`：系统通用组件与工具包（租户隔离、MyBatis 拦截器、安全加密等）
- `jianzao-modules`：系统基础能力模块（系统管理、代码生成、定时任务、工作流）
- `jianzao-extend`：扩展服务（监控服务与任务调度服务）

## 编译与打包

```bash
# 1. 编译并安装底座模块
mvn clean install -DskipTests -pl '!jianzao-admin'

# 2. 编译并打包启动工程 (winlocal profile)
mvn clean package -Pwinlocal -DskipTests -pl jianzao-admin -am
```
产物位置：`jianzao-admin/target/jianzao-admin.jar`
