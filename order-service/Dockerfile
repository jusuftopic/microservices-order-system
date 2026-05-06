# =========================
# BUILD STAGE
# =========================
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY order-service/pom.xml .
COPY order-service/src ./src

RUN mvn clean package -DskipTests

# =========================
# RUNTIME STAGE
# =========================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]