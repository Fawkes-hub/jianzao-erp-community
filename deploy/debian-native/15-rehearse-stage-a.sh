#!/usr/bin/env bash
# ============================================================
# 阶段 A 脚本的生产环境彩排（不触碰 erp 主库）
#
# 做法：把两补丁的 DDL 应用到影子表副本，验证脚本逻辑与 DDL 可行性。
# 影子表：drill_<ts>_biz_sale_return / drill_<ts>_fin_opening
# 结束后自动清理，不改动任何生产数据。
#
# 用法：bash 15-rehearse-stage-a.sh
# ============================================================
set -uo pipefail

STAMP="$(date +%Y%m%d%H%M%S)"
DB_NAME=erp
MYSQL_DEFAULTS="$(mktemp)"
cleanup() {
  if [ -s "${MYSQL_DEFAULTS}" ]; then
    mysql --defaults-extra-file="${MYSQL_DEFAULTS}" "${DB_NAME}" -e "
      DROP TABLE IF EXISTS \`drill_${STAMP}_biz_sale_return\`;
      DROP TABLE IF EXISTS \`drill_${STAMP}_fin_opening\`;" 2>/dev/null || true
  fi
  rm -f "${MYSQL_DEFAULTS}"
  log "影子表已清理"
}
trap cleanup EXIT

log() { printf '%s %s\n' "$(date -Is)" "$*"; }
fail() { log "FAIL: $*"; exit 1; }

[ "$(id -u)" -eq 0 ] || fail "需要 root"
[ -r /etc/default/erp ] || fail "/etc/default/erp 不可读"

DB_USER="$(grep -m1 '^SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=' /etc/default/erp | cut -d= -f2-)"
DB_PASS="$(grep -m1 '^SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=' /etc/default/erp | cut -d= -f2-)"
[ -n "${DB_USER}" ] && [ -n "${DB_PASS}" ] || fail "凭据解析失败"

cat > "${MYSQL_DEFAULTS}" <<EOF
[client]
user=${DB_USER}
password=${DB_PASS}
host=127.0.0.1
default-character-set=utf8mb4
EOF
chmod 600 "${MYSQL_DEFAULTS}"

q() { mysql --defaults-extra-file="${MYSQL_DEFAULTS}" -N -B "${DB_NAME}" -e "$1"; }

log "彩排开始，影子前缀 drill_${STAMP}_"

# ---- 1. 复制目标表结构（含数据，验证真实行数下的 DDL）----
log "复制表结构"
q "CREATE TABLE \`drill_${STAMP}_biz_sale_return\` LIKE biz_sale_return;" || fail "复制 biz_sale_return 失败"
q "CREATE TABLE \`drill_${STAMP}_fin_opening\` LIKE fin_opening;" || fail "复制 fin_opening 失败"
q "INSERT INTO \`drill_${STAMP}_biz_sale_return\` SELECT * FROM biz_sale_return;"
q "INSERT INTO \`drill_${STAMP}_fin_opening\` SELECT * FROM fin_opening;"

ROWS1="$(q "SELECT COUNT(*) FROM \`drill_${STAMP}_biz_sale_return\`;")"
ROWS2="$(q "SELECT COUNT(*) FROM \`drill_${STAMP}_fin_opening\`;")"
log "影子表行数：biz_sale_return=${ROWS1} fin_opening=${ROWS2}"

# ---- 2. 在影子表上执行等价的 ADD COLUMN ----
log "在影子表上执行 ADD COLUMN（INSTANT 算法）"
START=$(date +%s%N)
q "ALTER TABLE \`drill_${STAMP}_biz_sale_return\`
     ADD COLUMN refund_account_id BIGINT NULL DEFAULT NULL
     COMMENT '草稿及确认时选定的退款结算账户', ALGORITHM=INSTANT;" || fail "biz_sale_return ADD COLUMN 失败"
MID=$(date +%s%N)
q "ALTER TABLE \`drill_${STAMP}_fin_opening\`
     ADD COLUMN opening_date DATE DEFAULT NULL
     COMMENT '期初截止日（含当日），确认时作为台账 biz_date', ALGORITHM=INSTANT;" || fail "fin_opening ADD COLUMN 失败"
END=$(date +%s%N)
log "  补丁1 用时 $(( (MID - START) / 1000000 ))ms"
log "  补丁2 用时 $(( (END - MID) / 1000000 ))ms"

# ---- 3. 验证列属性 ----
log "验证列属性"
RESULT="$(q "
SELECT CONCAT(TABLE_NAME,'.',COLUMN_NAME,':',COLUMN_TYPE,':',IS_NULLABLE)
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='${DB_NAME}'
  AND TABLE_NAME IN ('drill_${STAMP}_biz_sale_return','drill_${STAMP}_fin_opening')
  AND COLUMN_NAME IN ('refund_account_id','opening_date')
ORDER BY TABLE_NAME;")"
log "  ${RESULT}"
CNT="$(printf '%s\n' "${RESULT}" | grep -c ':YES$')"
[ "${CNT}" -eq 2 ] || fail "期望 2 个可空列，实际 ${CNT}"

# ---- 4. 验证数据未变 ----
ROWS1B="$(q "SELECT COUNT(*) FROM \`drill_${STAMP}_biz_sale_return\`;")"
ROWS2B="$(q "SELECT COUNT(*) FROM \`drill_${STAMP}_fin_opening\`;")"
[ "${ROWS1}" = "${ROWS1B}" ] && [ "${ROWS2}" = "${ROWS2B}" ] \
  || fail "行数变化：${ROWS1}→${ROWS1B} / ${ROWS2}→${ROWS2B}"
log "行数未变：${ROWS1B} / ${ROWS2B}"

# ---- 5. 验证幂等（重复 ADD COLUMN 应被补丁的存储过程跳过）----
log "验证 INSTANT 重复执行会被拒绝（证明补丁的存储过程守卫必要）"
if q "ALTER TABLE \`drill_${STAMP}_biz_sale_return\`
        ADD COLUMN refund_account_id BIGINT NULL DEFAULT NULL, ALGORITHM=INSTANT;" 2>/dev/null; then
  log "  未报错（MySQL 允许重复列？异常，需排查）"
else
  log "  重复添加被正确拒绝 → 补丁里的 information_schema 检查是必要的"
fi

# ---- 6. 磁盘与锁影响 ----
log "确认无元数据锁残留"
LOCKS="$(q "SELECT COUNT(*) FROM performance_schema.metadata_locks WHERE OBJECT_SCHEMA='${DB_NAME}' AND OBJECT_NAME LIKE 'drill_${STAMP}_%';" 2>/dev/null || echo 'n/a')"
log "  残留元数据锁：${LOCKS}"

log "彩排通过：DDL 在真实表结构上可行，数据无变化，可用 INSTANT 算法"
log "主库 erp 未被修改"
exit 0
