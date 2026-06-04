# @path services/tenant-service/Dockerfile
# @owner tenant-service
# @responsibility Multi-stage build — imagem mínima com usuário não-root
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -f pom.xml dependency:go-offline -B --no-transfer-progress 2>/dev/null || true
COPY src ./src
RUN mvn -f pom.xml package -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}", "-jar", "app.jar"]
