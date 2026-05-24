# 运行阶段 - 使用本地构建的jar包
FROM eclipse-temurin:8-jre

WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 从本地复制jar包
COPY rbac-backend/target/rbac-server.jar app.jar

# 暴露端口
EXPOSE 8081

# JVM参数优化
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"

# 启动应用
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
