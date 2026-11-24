#!/usr/bin/env bash
# ============================================================
# 简造ERP 部署回滚
#
# 用法：bash 14-deploy-rollback.sh <留档时间戳>
#   时间戳由 13-deploy-stage-b-app.sh 输出（形如 20261123153045）
# 说明：数据库补丁不回滚。新增两列为可空且旧 jar 不读取，DROP 反而增风险。
# ============================================================
set -uo pipefail

STAMP="${1:-}"
APP_DIR=/opt/erp
WEB_DIR=/var/www/erp
HEALTH=http://127.0.0.1:8080/actuator/health

log() { printf '%s %s\n' "$(date -Is)" "$*"; }
fail() { log "FAIL: $*"; exit 1; }

[ -n "${STAMP}" ] || fail "用法：$0 <留档时间戳>（可 ls ${APP_DIR}/app.jar.rollback-* 查看）"
[ "$(id -u)" -eq 0 ] || fail "需要 root"

JAR_BACKUP="${APP_DIR}/app.jar.rollback-${STAMP}"
WEB_BACKUP="${WEB_DIR}.rollback-${STAMP}"

[ -f "${JAR_BACKUP}" ] || fail "旧 jar 留档不存在：${JAR_BACKUP}"

log "回滚锚点：${STAMP}"

# ---- 1. 校验旧 jar 完整性 ----
if [ -f "${APP_DIR}/rollback-${STAMP}.md5" ]; then
  log "校验旧 jar md5"
  EXPECTED_MD5="$(cut -d' ' -f1 "${APP_DIR}/rollback-${STAMP}.md5")"
  ACTUAL_MD5="$(md5sum "${JAR_BACKUP}" | cut -d' ' -f1)"
  [ "${EXPECTED_MD5}" = "${ACTUAL_MD5}" ] \
    || fail "旧 jar 校验不符：期望 ${EXPECTED_MD5} 实际 ${ACTUAL_MD5}"
  log "  校验通过"
fi

# ---- 2. 停服务 ----
log "停止 erp"
systemctl stop erp || fail "停止失败"

# ---- 3. 恢复 jar ----
log "恢复旧 jar"
install -o erp -g erp -m 644 "${JAR_BACKUP}" "${APP_DIR}/app.jar" || fail "jar 恢复失败"

# ---- 4. 恢复前端 ----
if [ -d "${WEB_BACKUP}" ]; then
  log "恢复旧前端"
  rm -rf "${WEB_DIR}.restore-tmp"
  mv "${WEB_DIR}" "${WEB_DIR}.broken-${STAMP}" 2>/dev/null || true
  cp -a "${WEB_BACKUP}" "${WEB_DIR}" || fail "前端恢复失败"
  rm -rf "${WEB_DIR}.broken-${STAMP}"
  chown -R www-data:www-data "${WEB_DIR}" 2>/dev/null || chown -R nginx:nginx "${WEB_DIR}" 2>/dev/null || true
else
  log "无前端留档，跳过（仅后端回滚）"
fi

# ---- 5. 启动并验证 ----
log "启动 erp"
systemctl start erp || fail "启动失败"

ready=0
for i in $(seq 1 60); do
  code="$(curl -s -o /dev/null -w '%{http_code}' "${HEALTH}" 2>/dev/null || echo 000)"
  if [ "${code}" = "200" ]; then
    log "回滚后就绪，用时约 $((i * 5)) 秒"
    ready=1
    break
  fi
  sleep 5
done
[ "${ready}" -eq 1 ] || fail "回滚后 300 秒内未就绪（${code}）"

nginx -t >/dev/null 2>&1 && systemctl reload nginx || true
SITE_CODE="$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8081/ 2>/dev/null || echo 000)"
log "前端 HTTP ${SITE_CODE}"

log "回滚完成。数据库新增列保留（可空，旧 jar 不读取，无副作用）"
exit 0
