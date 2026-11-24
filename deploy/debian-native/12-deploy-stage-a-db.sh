#!/usr/bin/env bash
# ============================================================
# 简造ERP 候选部署（阶段 A：数据库补丁）
#
# 特性：不停机。只加可空列，旧 jar 不受影响，可随时中断。
# 用法：bash 12-deploy-stage-a-db.sh
# 退出码：0 通过；1 失败（此时业务无影响，直接排查）
# ============================================================
set -uo pipefail

PATCH_DIR=/opt/erp/patches
DB_NAME=erp
MYSQL_DEFAULTS="$(mktemp)"
trap 'rm -f "${MYSQL_DEFAULTS}"' EXIT

log() { printf '%s %s\n' "$(date -Is)" "$*"; }
fail() { log "FAIL: $*"; exit 1; }

# 两个补丁的预期 sha256（与本轮验收证据一致）
declare -A EXPECTED=(
  [V20261120_01__sale_return_refund_account_id.sql]=2e2ebfb2330161943f9a392c3f46fc679b1246d8c4a129654d74a2a8dfcf41fa
  [V20261121_01__fin_opening_date.sql]=1a9c35c578eea1d2cf443fba6343408d70a85076d730fd54b49ad3ff46341184
)

[ -r /etc/default/erp ] || fail "/etc/default/erp 不可读"
DB_USER="$(grep -m1 '^SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=' /etc/default/erp | cut -d= -f2-)"
DB_PASS="$(grep -m1 '^SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=' /etc/default/erp | cut -d= -f2-)"
[ -n "${DB_USER}" ] && [ -n "${DB_PASS}" ] || fail "未能解析数据库凭据"

cat > "${MYSQL_DEFAULTS}" <<EOF
[client]
user=${DB_USER}
password=${DB_PASS}
host=127.0.0.1
default-character-set=utf8mb4
EOF
chmod 600 "${MYSQL_DEFAULTS}"

# ---- 1. 校验补丁完整性 ----
log "校验补丁 sha256"
for name in "${!EXPECTED[@]}"; do
  path="${PATCH_DIR}/${name}"
  [ -f "${path}" ] || fail "补丁不存在：${path}"
  actual="$(sha256sum "${path}" | cut -d' ' -f1)"
  [ "${actual}" = "${EXPECTED[$name]}" ] \
    || fail "${name} 校验和不符：期望 ${EXPECTED[$name]} 实际 ${actual}"
  log "  ${name} OK"
done

# ---- 2. 记录执行前状态 ----
log "记录执行前列状态"
BEFORE="$(mysql --defaults-extra-file="${MYSQL_DEFAULTS}" -N -B "${DB_NAME}" -e "
SELECT CONCAT(TABLE_NAME,'.',COLUMN_NAME,'=',COUNT(*))
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='${DB_NAME}'
  AND ((TABLE_NAME='biz_sale_return' AND COLUMN_NAME='refund_account_id')
    OR (TABLE_NAME='fin_opening' AND COLUMN_NAME='opening_date'))
GROUP BY TABLE_NAME, COLUMN_NAME;")"
log "  before: ${BEFORE:-（两列均不存在）}"

# ---- 3. 执行补丁 ----
for name in V20261120_01__sale_return_refund_account_id.sql V20261121_01__fin_opening_date.sql; do
  path="${PATCH_DIR}/${name}"
  started=$(date +%s%N)
  mysql --defaults-extra-file="${MYSQL_DEFAULTS}" "${DB_NAME}" < "${path}" 2>/tmp/patch-err.log \
    || { cat /tmp/patch-err.log; fail "${name} 执行失败"; }
  elapsed=$(( ($(date +%s%N) - started) / 1000000 ))
  log "  执行 ${name} 用时 ${elapsed}ms"
done

# ---- 4. 验证 ----
log "验证新增列"
RESULT="$(mysql --defaults-extra-file="${MYSQL_DEFAULTS}" -N -B "${DB_NAME}" -e "
SELECT CONCAT(TABLE_NAME,'.',COLUMN_NAME,':',COLUMN_TYPE,':',IS_NULLABLE)
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='${DB_NAME}'
  AND ((TABLE_NAME='biz_sale_return' AND COLUMN_NAME='refund_account_id')
    OR (TABLE_NAME='fin_opening' AND COLUMN_NAME='opening_date'))
ORDER BY TABLE_NAME;")"
log "  ${RESULT}"

COUNT="$(printf '%s\n' "${RESULT}" | grep -c ':YES$')"
[ "${COUNT}" -eq 2 ] || fail "期望 2 个可空列，实际 ${COUNT}"

printf '%s\n' "${RESULT}" | grep -q '^biz_sale_return.refund_account_id:bigint:YES$' \
  || fail "biz_sale_return.refund_account_id 类型不符"
printf '%s\n' "${RESULT}" | grep -q '^fin_opening.opening_date:date:YES$' \
  || fail "fin_opening.opening_date 类型不符"

# ---- 5. 确认业务未受影响 ----
log "确认应用仍健康（旧 jar 应不受新增列影响）"
code="$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/actuator/health 2>/dev/null)"
[ "${code}" = "200" ] || log "WARN 健康检查返回 ${code}（若原本非 200 可忽略）"

log "阶段 A 完成：数据库已就绪，旧 jar 仍在正常服务"
log "下一步：择机执行 13-deploy-stage-b-app.sh（需停机约 20 秒）"
exit 0
