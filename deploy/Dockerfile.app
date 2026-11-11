FROM eclipse-temurin:21-jre

WORKDIR /app

# 生产 VPS 无法直连 archive.ubuntu.com / security.ubuntu.com（80 与 443 均超时），
# 默认走阿里云镜像；其他网络环境可用 --build-arg APT_MIRROR=archive.ubuntu.com 覆盖。
# curl 供容器健康检查，default-mysql-client 供应用内定时备份调用 mysqldump，均不可省略。
ARG APT_MIRROR=mirrors.aliyun.com
RUN set -eux; \
    for f in /etc/apt/sources.list /etc/apt/sources.list.d/ubuntu.sources; do \
      [ -f "$f" ] || continue; \
      sed -i "s|http://archive.ubuntu.com/ubuntu|https://${APT_MIRROR}/ubuntu|g; \
              s|http://security.ubuntu.com/ubuntu|https://${APT_MIRROR}/ubuntu|g" "$f"; \
    done; \
    apt-get update; \
    apt-get install -y --no-install-recommends curl default-mysql-client; \
    rm -rf /var/lib/apt/lists/*; \
    command -v curl >/dev/null; \
    mysqldump --version

ARG JAR_FILE=app/erp.jar
COPY ${JAR_FILE} /app/app.jar

RUN mkdir -p /app/data /app/data/temp

EXPOSE 8080
ENV TZ=Asia/Shanghai

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
