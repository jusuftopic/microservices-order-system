# =========================
# BUILD STAGE
# =========================
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent POM first (important for multi-module Maven)
COPY pom.xml .

# Copy module source
COPY order-service/pom.xml order-service/pom.xml
COPY order-service/src order-service/src

RUN mvn -pl order-service -am clean package -DskipTests

# =========================
# RUNTIME STAGE
# =========================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]