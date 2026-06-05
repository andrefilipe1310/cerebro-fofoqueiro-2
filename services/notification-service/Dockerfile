# @path services/notification-service/Dockerfile
# @owner notification-service
# @responsibility Multi-stage build — imagem mínima com usuário não-root
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress -q
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}", "-jar", "app.jar"]
