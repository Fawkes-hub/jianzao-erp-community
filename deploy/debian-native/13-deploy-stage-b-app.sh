#!/usr/bin/env bash
# ============================================================
# 简造ERP 候选部署（阶段 B+C：后端与前端切换）
#
# 前提：已执行 12-deploy-stage-a-db.sh 并通过
# 停机：约 30 秒（后端 18s 启动 + 前端原子替换）
# 回滚：脚本自动记录留档；失败时按提示执行 14-deploy-rollback.sh
# 用法：bash 13-deploy-stage-b-app.sh /tmp/app.jar.new /tmp/dist.new
# ============================================================
set -uo pipefail

NEW_JAR="${1:-}"
NEW_DIST="${2:-}"
STAMP="$(date +%Y%m%d%H%M%S)"
APP_DIR=/opt/erp
WEB_DIR=/var/www/erp
HEALTH=http://127.0.0.1:8080/actuator/health
SITE=http://127.0.0.1:8081/

log() { printf '%s %s\n' "$(date -Is)" "$*"; }
fail() { log "FAIL: $*"; log "回滚：bash /opt/erp/14-deploy-rollback.sh ${STAMP}"; exit 1; }

[ -n "${NEW_JAR}" ] && [ -f "${NEW_JAR}" ] || fail "用法：$0 <新jar> <新dist目录>"
[ -n "${NEW_DIST}" ] && [ -d "${NEW_DIST}" ] || fail "前端目录不存在：${NEW_DIST}"
[ "$(id -u)" -eq 0 ] || fail "需要 root"

# ---- 0. 留档（回滚物资）----
log "留档旧产物（stamp=${STAMP}）"
cp -a "${APP_DIR}/app.jar" "${APP_DIR}/app.jar.rollback-${STAMP}" || fail "jar 留档失败"
if [ -d "${WEB_DIR}" ]; then
  cp -a "${WEB_DIR}" "${WEB_DIR}.rollback-${STAMP}" || fail "前端留档失败"
fi
md5sum "${APP_DIR}/app.jar.rollback-${STAMP}" | tee "${APP_DIR}/rollback-${STAMP}.md5"
log "留档完成；回滚锚点 ${STAMP}"

# ---- 1. 部署前健康基线 ----
BASE_CODE="$(curl -s -o /dev/null -w '%{http_code}' "${HEALTH}" 2>/dev/null || echo 000)"
log "部署前健康检查：${BASE_CODE}"
[ "${BASE_CODE}" = "200" ] || [ "${BASE_CODE}" = "401" ]   || log "WARN 部署前健康非 200/401（${BASE_CODE}），继续但需留意"

# ---- 2. 停服务 ----
log "停止 erp 服务"
systemctl stop erp || fail "停止失败"

# ---- 3. 换 jar ----
log "替换后端 jar"
install -o erp -g erp -m 644 "${NEW_JAR}" "${APP_DIR}/app.jar" || {
  log "替换失败，立即恢复旧 jar"
  cp -a "${APP_DIR}/app.jar.rollback-${STAMP}" "${APP_DIR}/app.jar"
  systemctl start erp
  fail "jar 替换失败"
}

# ---- 4. 换前端（原子替换）----
log "替换前端静态资源"
rm -rf "${WEB_DIR}.new"
cp -a "${NEW_DIST}" "${WEB_DIR}.new" || fail "前端复制失败"
if [ -d "${WEB_DIR}" ]; then
  mv "${WEB_DIR}" "${WEB_DIR}.old-${STAMP}"
fi
mv "${WEB_DIR}.new" "${WEB_DIR}" || {
  log "前端切换失败，恢复"
  [ -d "${WEB_DIR}.old-${STAMP}" ] && mv "${WEB_DIR}.old-${STAMP}" "${WEB_DIR}"
  cp -a "${APP_DIR}/app.jar.rollback-${STAMP}" "${APP_DIR}/app.jar"
  systemctl start erp
  fail "前端切换失败"
}
chown -R www-data:www-data "${WEB_DIR}" 2>/dev/null || chown -R nginx:nginx "${WEB_DIR}" 2>/dev/null || true
rm -rf "${WEB_DIR}.old-${STAMP}"

# ---- 5. 启动 ----
log "启动 erp 服务"
systemctl start erp || fail "启动失败"

# ---- 6. 等待就绪（实测约 18 秒，上限 300 秒）----
log "等待后端就绪"
ready=0
for i in $(seq 1 60); do
  code="$(curl -s -o /dev/null -w '%{http_code}' "${HEALTH}" 2>/dev/null || echo 000)"
  if [ "${code}" = "200" ] || [ "${code}" = "401" ]; then
    log "后端就绪（HTTP ${code}），用时约 $((i * 5)) 秒"
    ready=1
    break
  fi
  sleep 5
done
[ "${ready}" -eq 1 ] || fail "300 秒内未就绪（最后状态 ${code}）"

# ---- 7. 验证 ----
log "验证监听面（应全部回环）"
LISTEN="$(ss -tln 2>/dev/null | grep -E ':(8080|3306|6379)\b' || true)"
printf '%s\n' "${LISTEN}"
if printf '%s\n' "${LISTEN}" | grep -qvE '127\.0\.0\.1|\[::1\]'; then
  log "WARN 检测到非回环监听，请核对："
  printf '%s\n' "${LISTEN}" | grep -vE '127\.0\.0\.1|\[::1\]'
fi

log "验证前端"
nginx -t 2>&1 | tail -1
systemctl reload nginx || log "WARN nginx reload 失败"
SITE_CODE="$(curl -s -o /dev/null -w '%{http_code}' "${SITE}" 2>/dev/null || echo 000)"
log "前端 HTTP ${SITE_CODE}"
[ "${SITE_CODE}" = "200" ] || fail "前端不可达（${SITE_CODE}）"

log "检查启动后 ERROR"
ERR_COUNT="$(journalctl -u erp --since '5 min ago' --no-pager 2>/dev/null | grep -c ' ERROR ' || true)"
log "近 5 分钟 ERROR 行数：${ERR_COUNT}"
[ "${ERR_COUNT}" -lt 20 ] || log "WARN ERROR 较多，请查看 journalctl -u erp"

log "部署完成。回滚锚点：${STAMP}"
log "如后续需回滚：bash /opt/erp/14-deploy-rollback.sh ${STAMP}"
exit 0
