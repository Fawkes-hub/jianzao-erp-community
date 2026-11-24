#!/usr/bin/env bash
# ============================================================
# 简造ERP 备份恢复验证（同库影子前缀演练，不覆盖生产数据）
#
# 背景：应用账号 erp@127.0.0.1 仅有 `erp` 库的权限，无 CREATE DATABASE。
#      演练因此不建临时库，而是把备份还原到同一库的 `drill_` 前缀表，
#      逐表比对行数后立即 DROP。生产表全程只读，不被写入。
#
# 用法：bash 11-restore-drill.sh [YYYYMMDD]
#       省略参数时使用最新一份备份
# 退出码：0 演练通过；1 备份缺失/还原失败/校验不一致
# ============================================================
set -uo pipefail

BACKUP_ROOT=/opt/erp/backup
PREFIX="drill_$(date +%s)_"
MYSQL_DEFAULTS="$(mktemp)"
DRILL_TABLES=()

cleanup() {
  for t in "${DRILL_TABLES[@]:-}"; do
    [ -n "${t}" ] || continue
    mysql --defaults-extra-file="${MYSQL_DEFAULTS}" erp -e "DROP TABLE IF EXISTS \`${t}\`" 2>/dev/null
  done
  rm -f "${MYSQL_DEFAULTS}"
}
trap cleanup EXIT

log() { printf '%s %s\n' "$(date -Is)" "$*"; }
fail() { log "FAIL: $*"; exit 1; }

[ -r /etc/default/erp ] || fail "/etc/default/erp 不可读"
DB_USER="$(grep -m1 '^SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=' /etc/default/erp | cut -d= -f2-)"
DB_PASS="$(grep -m1 '^SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=' /etc/default/erp | cut -d= -f2-)"
[ -n "${DB_USER}" ] && [ -n "${DB_PASS}" ] || fail "未能解析数据库凭据"

cat > "${MYSQL_DEFAULTS}" <<EOF
[client]
user=${DB_USER}
password=${DB_PASS}
host=127.0.0.1
EOF
chmod 600 "${MYSQL_DEFAULTS}"

STAMP="${1:-}"
if [ -z "${STAMP}" ]; then
  STAMP="$(find "${BACKUP_ROOT}" -maxdepth 1 -type d -name '20*' | sort | tail -1 | xargs -r basename)"
fi
[ -n "${STAMP}" ] || fail "找不到任何备份目录"
DUMP="${BACKUP_ROOT}/${STAMP}/erp.sql.gz"
[ -s "${DUMP}" ] || fail "备份文件不存在或为空：${DUMP}"

log "演练备份：${DUMP}"
gzip -t "${DUMP}" || fail "备份文件损坏"
( cd "${BACKUP_ROOT}/${STAMP}" && sha256sum -c SHA256SUMS --quiet ) || fail "校验清单不一致"

# ---- 把 dump 里的 CREATE TABLE / INSERT INTO 改写为影子前缀表 ----
# 只处理裸表名（不含库名限定），DROP TABLE IF EXISTS 同样改写，避免误删生产表。
log "生成影子前缀 SQL（前缀 ${PREFIX}）"
REWRITTEN="$(mktemp)"
cleanup_sql() { rm -f "${REWRITTEN}"; }
trap 'cleanup_sql; cleanup' EXIT

gunzip -c "${DUMP}" | sed -E \
  -e 's/^CREATE TABLE `([^`]+)`/CREATE TABLE `'"${PREFIX}"'\1`/' \
  -e 's/^DROP TABLE IF EXISTS `([^`]+)`/DROP TABLE IF EXISTS `'"${PREFIX}"'\1`/' \
  -e 's/^INSERT INTO `([^`]+)`/INSERT INTO `'"${PREFIX}"'\1`/' \
  -e 's/^LOCK TABLES `([^`]+)`/LOCK TABLES `'"${PREFIX}"'\1`/' \
  -e 's/^\/\*!40000 ALTER TABLE `([^`]+)`/\/\*!40000 ALTER TABLE `'"${PREFIX}"'\1`/' \
  > "${REWRITTEN}" || fail "SQL 改写失败"

grep -c "^CREATE TABLE \`${PREFIX}" "${REWRITTEN}" >/dev/null 2>&1 || fail "改写后未发现影子表定义"

log "还原影子表"
mysql --defaults-extra-file="${MYSQL_DEFAULTS}" erp < "${REWRITTEN}" 2>/dev/null || fail "影子表还原失败"

# ---- 收集影子表清单 ----
mapfile -t DRILL_TABLES < <(mysql --defaults-extra-file="${MYSQL_DEFAULTS}" -N -e \
  "SELECT table_name FROM information_schema.tables WHERE table_schema='erp' AND table_name LIKE '${PREFIX}%' ORDER BY table_name")
[ "${#DRILL_TABLES[@]}" -gt 0 ] || fail "影子表未创建"

# ---- 逐表比对生产表与影子表 ----
MISMATCH=0
for t in "${DRILL_TABLES[@]}"; do
  prod="${t#"${PREFIX}"}"
  prod_cnt="$(mysql --defaults-extra-file="${MYSQL_DEFAULTS}" -N -e "SELECT COUNT(*) FROM \`erp\`.\`${prod}\`" 2>/dev/null)"
  drill_cnt="$(mysql --defaults-extra-file="${MYSQL_DEFAULTS}" -N -e "SELECT COUNT(*) FROM \`erp\`.\`${t}\`" 2>/dev/null)"
  if [ "${prod_cnt}" != "${drill_cnt}" ]; then
    log "行数不一致 ${prod}: 生产=${prod_cnt} 还原=${drill_cnt}"
    MISMATCH=$((MISMATCH + 1))
  fi
done

log "比对完成：${#DRILL_TABLES[@]} 张表，不一致 ${MISMATCH} 张"
[ "${MISMATCH}" -eq 0 ] || fail "还原结果与生产不一致"

log "演练通过，影子表将被删除"
exit 0
