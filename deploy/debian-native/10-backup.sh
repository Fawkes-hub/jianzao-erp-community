#!/usr/bin/env bash
# ============================================================
# 简造ERP 生产每日备份
#
# 内容：MySQL erp 库逻辑备份 + /opt/erp 配置与附件 + Nginx 站点配置
# 保留：7 天（按日期目录，超期自动清理）
# 位置：/opt/erp/backup/YYYYMMDD/
#
# 依赖：/etc/default/erp 提供数据库口令（600 root:root）
# 用法：由 /etc/cron.d/erp-backup 每日 03:20 触发，也可手工执行
# 退出码：0 全部成功；1 任一子项失败（已写入日志）
# ============================================================
set -uo pipefail

BACKUP_ROOT=/opt/erp/backup
KEEP_DAYS=7
STAMP="$(date +%Y%m%d)"
DEST="${BACKUP_ROOT}/${STAMP}"
LOG="${BACKUP_ROOT}/backup.log"
MYSQL_DEFAULTS="$(mktemp)"
trap 'rm -f "${MYSQL_DEFAULTS}"' EXIT

log() { printf '%s %s\n' "$(date -Is)" "$*" | tee -a "${LOG}"; }
fail() { log "FAIL: $*"; exit 1; }

mkdir -p "${BACKUP_ROOT}" "${DEST}" || fail "无法创建备份目录"
chmod 700 "${BACKUP_ROOT}" "${DEST}"

# ---- 读取数据库口令（不落命令行，避免 ps 泄露）----
[ -r /etc/default/erp ] || fail "/etc/default/erp 不可读"
DB_USER="$(grep -m1 '^SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=' /etc/default/erp | cut -d= -f2-)"
DB_PASS="$(grep -m1 '^SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=' /etc/default/erp | cut -d= -f2-)"
[ -n "${DB_USER}" ] && [ -n "${DB_PASS}" ] || fail "未能从 /etc/default/erp 解析数据库凭据"

cat > "${MYSQL_DEFAULTS}" <<EOF
[client]
user=${DB_USER}
password=${DB_PASS}
host=127.0.0.1
EOF
chmod 600 "${MYSQL_DEFAULTS}"

# ---- 1. 数据库逻辑备份 ----
log "开始备份 erp 数据库"
mysqldump --defaults-extra-file="${MYSQL_DEFAULTS}" \
  --single-transaction --quick --routines --triggers --events \
  --default-character-set=utf8mb4 \
  erp 2>>"${LOG}" | gzip -9 > "${DEST}/erp.sql.gz" \
  || fail "mysqldump 失败"
[ -s "${DEST}/erp.sql.gz" ] || fail "数据库备份为空"
gzip -t "${DEST}/erp.sql.gz" || fail "数据库备份损坏"
log "数据库备份完成 $(du -h "${DEST}/erp.sql.gz" | cut -f1)"

# ---- 2. 应用配置与上传附件 ----
log "开始备份配置与附件"
tar -czf "${DEST}/erp-config.tar.gz" \
  -C / etc/default/erp etc/systemd/system/erp.service etc/redis/redis-erp.conf \
  etc/nginx/sites-available/erp-8081 2>>"${LOG}" \
  || fail "配置文件打包失败"
if [ -d /opt/erp/upload ]; then
  tar -czf "${DEST}/erp-upload.tar.gz" -C /opt/erp upload 2>>"${LOG}" \
    || fail "附件打包失败"
else
  log "无 /opt/erp/upload 目录，跳过附件"
fi
log "配置与附件备份完成"

# ---- 3. 生成校验清单 ----
( cd "${DEST}" && sha256sum ./* > SHA256SUMS 2>/dev/null ) || fail "生成校验清单失败"

# 备份内含数据库口令与附件，文件本身也收紧为仅 root 可读
chmod 600 "${DEST}"/* 2>/dev/null || fail "收紧备份文件权限失败"

# ---- 4. 清理超期备份 ----
find "${BACKUP_ROOT}" -maxdepth 1 -type d -name '20*' -mtime "+${KEEP_DAYS}" -print 2>/dev/null |
while read -r old; do
  log "清理超期备份 ${old}"
  rm -rf -- "${old}"
done

log "备份成功 ${DEST}"
exit 0
