# @path services/auth-service/Dockerfile
# @owner auth-service
# @responsibility Multi-stage build — imagem mínima com usuário não-root
# @see docs/TECH_STACK.md#stack-completo (Java 21)

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
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} -jar app.jar"]
