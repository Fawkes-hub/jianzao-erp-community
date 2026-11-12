# MySQL 初始化脚本

MySQL 容器**只在数据卷为空时**执行本目录下的 `*.sql`。

首次启动前，把以下脚本按文件名顺序放入本目录：

1. `01-ruoyi-system.sql`：RuoYi-Vue-Plus 系统表/菜单/字典/默认管理员。
2. `04-erp-schema.sql`：ERP 业务表、索引。
3. `05-erp-seed.sql`：ERP 菜单、按钮权限、字典、默认仓库。
4. `06-menu-icons-tdesign.sql`：TDesign 菜单图标。

> 已裁剪 `ry_job.sql`（任务调度）与 `ry_workflow.sql`（工作流）：对应依赖已从 `ruoyi-admin/pom.xml` 移除，v1 不使用。

注意：

- 如果 MySQL 数据卷已存在，新增 SQL 不会自动执行；请用迁移脚本或手工导入。
- 重新初始化会删除数据：`docker compose down -v`（危险，仅测试环境）。
- 生产环境升级只增加迁移脚本，不修改已执行脚本。
