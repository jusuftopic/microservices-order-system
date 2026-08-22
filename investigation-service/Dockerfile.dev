# =========================
# BUILD STAGE
# =========================
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY . .

RUN mvn -f pom.xml clean package -DskipTests

# =========================
# RUNTIME STAGE
# =========================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -D -S appuser

COPY --from=build /app/investigation-service/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER 1000

EXPOSE 8085
EXPOSE 8185

ENTRYPOINT ["java", "-jar", "app.jar"]
