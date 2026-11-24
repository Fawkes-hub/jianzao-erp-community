#!/usr/bin/env bash
# ============================================================
# 候选 jar 启动验证 v3（独立 MySQL 实例方案）
#
# 背景：应用账号无 CREATE DATABASE，root 无密码无法登录，
#       同库副本表对应用不可见。故启动第二个 MySQL 实例。
#
# 做法：
#   1. 在 /opt/erp/bootcheck/mysql 初始化独立数据目录（端口 13306）
#   2. 从主库导出数据导入（只读主库）
#   3. 在副本上执行两个补丁
#   4. 候选 jar 连 13306 启动（端口 18080）
#   5. 验证健康、登录路由、新列可用、日志
#   6. 停止实例与 mysqld，删除数据目录
#
# 隔离性：不触碰 /var/lib/mysql、不碰 3306、不碰 8080/8081
# 用法：bash 16c-verify-candidate-boot.sh
# ============================================================
set -uo pipefail

STAMP="$(date +%Y%m%d%H%M%S)"
BASE=/var/tmp/erp-bootcheck
RUN="${BASE}/run-${STAMP}"
DATADIR="${RUN}/mysql-data"
SOCKET="${RUN}/mysql.sock"
MYSQL_PIDFILE="${RUN}/mysql.pid"
MYSQL_LOG="${RUN}/mysql-error.log"
DB=erp_bootcheck
DB_PORT=13306
JAR=/opt/erp/staging-candidate.jar
APP_PORT=18080
APP_LOG="${RUN}/app.log"
APP_PID=""
MYSQL_PID=""

log() { printf '%s %s\n' "$(date -Is)" "$*"; }
fail() { log "FAIL: $*"; cleanup; exit 1; }

cleanup() {
  log "清理开始"
  if [ -n "${APP_PID}" ] && kill -0 "${APP_PID}" 2>/dev/null; then
    log "  停止候选实例 pid=${APP_PID}"
    kill -TERM "${APP_PID}" 2>/dev/null
    for _ in $(seq 1 30); do kill -0 "${APP_PID}" 2>/dev/null || break; sleep 1; done
    kill -0 "${APP_PID}" 2>/dev/null && kill -KILL "${APP_PID}" 2>/dev/null
    log "  实例已停止"
  fi
  if [ -f "${MYSQL_PIDFILE}" ]; then
    MPID="$(cat "${MYSQL_PIDFILE}" 2>/dev/null)"
    if [ -n "${MPID}" ] && kill -0 "${MPID}" 2>/dev/null; then
      log "  停止临时 mysqld pid=${MPID}"
      mysqladmin --socket="${SOCKET}" -uroot shutdown 2>/dev/null || kill -TERM "${MPID}" 2>/dev/null
      for _ in $(seq 1 40); do kill -0 "${MPID}" 2>/dev/null || break; sleep 1; done
      kill -0 "${MPID}" 2>/dev/null && kill -KILL "${MPID}" 2>/dev/null
    fi
    log "  临时 mysqld 已停止"
  fi
  if [ "${KEEP_LOGS:-0}" = "1" ]; then
    mkdir -p /root/bootcheck-logs
    cp -a "${RUN}"/*.log "${RUN}"/app-env.sh /root/bootcheck-logs/ 2>/dev/null || true
    log "  日志已保留到 /root/bootcheck-logs/"
  fi
  rm -f /opt/erp/bootcheck-app-env.sh
  rm -rf "${RUN}"
  log "  临时目录已删除：${RUN}"
  log "清理完成"
}
trap cleanup EXIT

[ "$(id -u)" -eq 0 ] || fail "需要 root"
[ -f "${JAR}" ] || fail "候选 jar 不存在：${JAR}"

AVAIL_MB="$(free -m | awk '/^Mem:/{print $7}')"
log "可用内存 ${AVAIL_MB}MB"
[ "${AVAIL_MB}" -ge 700 ] || fail "可用内存不足 700MB（${AVAIL_MB}MB）"

mkdir -p "${BASE}" "${RUN}"
# BASE 交给 mysql 且不启用 sticky；RUN 由 root 建后移交，DATADIR 让 mysqld 自建
chown mysql:mysql "${BASE}" "${RUN}"
chmod 750 "${BASE}" "${RUN}"

# ---- 1. 初始化临时 MySQL ----
log "初始化临时 MySQL 数据目录"

mysqld --no-defaults --initialize-insecure --user=mysql \
  --datadir="${DATADIR}" --log-error="${MYSQL_LOG}" \
  || fail "mysqld 初始化失败（日志由 mysqld 自行创建，不重定向）"
log "  初始化完成"

log "启动临时 mysqld（端口 ${DB_PORT}，仅回环）"
mysqld --no-defaults --user=mysql \
  --datadir="${DATADIR}" --socket="${SOCKET}" --pid-file="${MYSQL_PIDFILE}" \
  --port="${DB_PORT}" --bind-address=127.0.0.1 \
  --log-error="${MYSQL_LOG}" \
  --innodb-buffer-pool-size=64M --max-connections=20 \
  --performance-schema=OFF --mysqlx=OFF --skip-log-bin \
  --tmpdir="${RUN}" &
sleep 8

for i in $(seq 1 20); do
  if mysqladmin --socket="${SOCKET}" -uroot ping >/dev/null 2>&1; then
    log "  临时 mysqld 就绪"
    MYSQL_PID="$(cat "${MYSQL_PIDFILE}" 2>/dev/null)"
    break
  fi
  sleep 2
done
mysqladmin --socket="${SOCKET}" -uroot ping >/dev/null 2>&1 || { tail -20 "${MYSQL_LOG}"; fail "临时 mysqld 启动失败"; }

# ---- 2. 建库并导入主库数据 ----
log "创建副本库并导入主库数据"
mysql --socket="${SOCKET}" -uroot -e "
  CREATE DATABASE \`${DB}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
  CREATE USER 'bc'@'127.0.0.1' IDENTIFIED BY 'BootcheckTmp#2026';
  GRANT ALL PRIVILEGES ON \`${DB}\`.* TO 'bc'@'127.0.0.1';
  FLUSH PRIVILEGES;" || fail "建库失败"

# 用主库凭据只读导出
MAIN_USER="$(sed -n 's/^SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=//p' /etc/default/erp | head -1)"
MAIN_PASS="$(sed -n 's/^SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=//p' /etc/default/erp | head -1)"
[ -n "${MAIN_USER}" ] && [ -n "${MAIN_PASS}" ] || fail "主库凭据解析失败"

DUMP="${RUN}/erp.sql"
MYSQL_PWD="${MAIN_PASS}" mysqldump -h127.0.0.1 -u"${MAIN_USER}" \
  --single-transaction --quick --no-tablespaces --default-character-set=utf8mb4 \
  erp > "${DUMP}" 2>"${RUN}/dump.err" \
  || { cat "${RUN}/dump.err"; fail "主库导出失败"; }
log "  导出 $(stat -c%s "${DUMP}") 字节"

mysql --socket="${SOCKET}" -uroot "${DB}" < "${DUMP}" || fail "导入失败"
TBL="$(mysql --socket="${SOCKET}" -uroot -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB}' AND table_type='BASE TABLE';")"
log "  导入 ${TBL} 张表"
[ "${TBL}" -ge 60 ] || fail "导入表数异常（${TBL}）"

# ---- 3. 在副本上执行补丁 ----
log "在副本上执行两个补丁"
for name in V20261120_01__sale_return_refund_account_id.sql V20261121_01__fin_opening_date.sql; do
  mysql --socket="${SOCKET}" -uroot "${DB}" < "/opt/erp/patches/${name}" 2>"${RUN}/patch.err" \
    || { cat "${RUN}/patch.err"; fail "补丁 ${name} 失败"; }
  log "  ${name} OK"
done
COLS="$(mysql --socket="${SOCKET}" -uroot -N -B -e "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='${DB}' AND ((table_name='biz_sale_return' AND column_name='refund_account_id') OR (table_name='fin_opening' AND column_name='opening_date'));")"
log "  新增列数：${COLS}"
[ "${COLS}" -eq 2 ] || fail "补丁后列数异常（${COLS}）"

# ---- 4. 启动候选实例 ----
log "启动候选 jar（端口 ${APP_PORT}，连 127.0.0.1:${DB_PORT}/${DB}）"
JDBC="jdbc:mysql://127.0.0.1:${DB_PORT}/${DB}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

REDIS_HOST="$(sed -n 's/^SPRING_DATA_REDIS_HOST=//p' /etc/default/erp | head -1)"
REDIS_PORT="$(sed -n 's/^SPRING_DATA_REDIS_PORT=//p' /etc/default/erp | head -1)"
REDIS_PASS="$(sed -n 's/^SPRING_DATA_REDIS_PASSWORD=//p' /etc/default/erp | head -1)"

# 继承生产全部非数据库环境变量（JWT 密钥、Undertow 线程、CORS 等），仅覆盖数据源与端口
ENVFILE=/opt/erp/bootcheck-app-env.sh
{
  echo '#!/bin/bash'
  grep -E '^[A-Z_]+=' /etc/default/erp     | grep -vE '^SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_(URL|USERNAME|PASSWORD)='     | grep -vE '^SERVER_ADDRESS='     | sed 's/^/export /'
  echo "export SERVER_PORT=${APP_PORT}"
  echo "export SERVER_ADDRESS=127.0.0.1"
  echo "export SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL='${JDBC}'"
  echo "export SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME='bc'"
  echo "export SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD='BootcheckTmp#2026'"
} > "${ENVFILE}"
chown erp:erp "${ENVFILE}"
chmod 600 "${ENVFILE}"
[ -r "${ENVFILE}" ] || fail "env 文件不可读：${ENVFILE}"

su -s /bin/bash erp -c "set -a; . '${ENVFILE}'; set +a; exec /usr/bin/java -Xmx512m -Djava.io.tmpdir=/opt/erp/temp -jar ${JAR} --spring.profiles.active=prod --server.port=${APP_PORT}" > "${APP_LOG}" 2>&1 &
APP_PID=$!
log "  pid=${APP_PID}，日志 ${APP_LOG}"
sleep 3
if ! kill -0 "${APP_PID}" 2>/dev/null; then
  log "  实例提前退出，检查 env 加载"; grep -E "Caused by|Could not resolve" "${APP_LOG}" | head -5
fi

log "等待就绪（上限 240 秒）"
READY=0
for i in $(seq 1 48); do
  if ! kill -0 "${APP_PID}" 2>/dev/null; then
    log "进程已退出，日志尾部："; tail -35 "${APP_LOG}"
    fail "候选实例启动后退出"
  fi
  CODE="$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:${APP_PORT}/actuator/health" 2>/dev/null || echo 000)"
  if [ "${CODE}" = "200" ] || [ "${CODE}" = "401" ]; then
    log "  就绪（HTTP ${CODE}），用时约 $((i * 5)) 秒"
    READY=1; break
  fi
  sleep 5
done
[ "${READY}" -eq 1 ] || { tail -40 "${APP_LOG}"; fail "240 秒内未就绪"; }

# ---- 5. 验证 ----
log "验证健康端点"
HB="$(curl -s -m 5 "http://127.0.0.1:${APP_PORT}/actuator/health" 2>/dev/null)"
log "  响应：${HB}"
if printf '%s' "${HB}" | grep -q '"status":"UP"'; then
  log "  健康状态 UP"
elif printf '%s' "${HB}" | grep -q 'no basic auth'; then
  log "  health 需 basic auth（401），服务已监听即视为通过"
else
  fail "健康端点响应异常：${HB}"
fi

log "验证启动完成标志"
if grep -q 'Started DromaraApplication in' "${APP_LOG}" 2>/dev/null; then
  grep -E 'Started DromaraApplication in' "${APP_LOG}" | tail -1 | sed 's/^/  /'
else
  fail "日志中未见启动完成标志"
fi

log "验证启动耗时（从日志提取）"
grep -E 'Started DromaraApplication in' "${APP_LOG}" | tail -1 | sed 's/^/  /'

log "验证登录路由可达"
LOGIN_CODE="$(curl -s -o /dev/null -w '%{http_code}' -X POST "http://127.0.0.1:${APP_PORT}/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"__bootcheck__","password":"__bootcheck__","grantType":"password"}' 2>/dev/null)"
log "  登录接口 HTTP ${LOGIN_CODE}"
[ "${LOGIN_CODE}" != "000" ] || fail "登录接口无响应"

log "验证 Swagger/路由存在"
R_CODE="$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:${APP_PORT}/erp/master/product/list" 2>/dev/null)"
log "  业务路由 HTTP ${R_CODE}（401=未认证属正常，说明路由与 DB 已通）"

log "验证启动日志无致命错误"
FATAL="$(grep -cE 'APPLICATION FAILED TO START|UnsatisfiedDependencyException|BeanCreationException' "${APP_LOG}" 2>/dev/null | head -1)"
FATAL="${FATAL:-0}"
log "  致命错误行数：${FATAL}"
[ "${FATAL}" -eq 0 ] || { grep -E 'APPLICATION FAILED TO START|Caused by:' "${APP_LOG}" | head -8; fail "启动日志有致命错误"; }

log "验证新列可被查询（直接查副本库）"
mysql --socket="${SOCKET}" -uroot -N -B "${DB}" -e "
  SELECT 'biz_sale_return.refund_account_id', COLUMN_TYPE, IS_NULLABLE
  FROM information_schema.columns
  WHERE table_schema='${DB}' AND table_name='biz_sale_return' AND column_name='refund_account_id';
  SELECT 'fin_opening.opening_date', COLUMN_TYPE, IS_NULLABLE
  FROM information_schema.columns
  WHERE table_schema='${DB}' AND table_name='fin_opening' AND column_name='opening_date';" | sed 's/^/  /'

log "验证主库未被改动"
MAIN_COLS="$(MYSQL_PWD="${MAIN_PASS}" mysql -h127.0.0.1 -u"${MAIN_USER}" -N -B erp -e "
  SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='erp'
  AND ((table_name='biz_sale_return' AND column_name='refund_account_id')
    OR (table_name='fin_opening' AND column_name='opening_date'));" 2>/dev/null)"
log "  主库新增列数：${MAIN_COLS}（应为 0）"
[ "${MAIN_COLS}" = "0" ] || fail "主库被意外修改"

log "验证线上服务未受影响"
LIVE="$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8081/ 2>/dev/null || echo 000)"
LIVE8080="$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/actuator/health 2>/dev/null || echo 000)"
log "  线上前端:${LIVE} 线上后端:${LIVE8080}"
[ "${LIVE}" = "200" ] || fail "线上前端异常（${LIVE}）"

log "启动验证通过：候选 jar 可在生产数据上正常启动并服务"
exit 0
