# backend 镜像（需先执行: cd backend && mvn clean package -DskipTests）
FROM eclipse-temurin:17-jre

WORKDIR /app

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY backend/target/backend.jar app.jar

EXPOSE 8081

ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
