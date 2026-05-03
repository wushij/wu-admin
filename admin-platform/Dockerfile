# 运行阶段 - 使用本地构建的jar包
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 从本地复制jar包
COPY admin-gateway/target/admin-gateway-1.0.0.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM参数优化
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"

# 启动应用
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
