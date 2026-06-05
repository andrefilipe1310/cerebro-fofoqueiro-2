# @path services/auth-service/Dockerfile
# @owner auth-service
# @responsibility Multi-stage build — imagem mínima com usuário não-root
# @see docs/TECH_STACK.md#stack-completo (Java 21)

# Stage 1: build — Maven incluso nesta imagem
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
# Cache de dependências em layer separada (invalida só quando pom.xml muda)
RUN mvn dependency:go-offline -B --no-transfer-progress -q
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress -q

# Stage 2: runtime (imagem mínima)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}", \
  "-jar", "app.jar"]
