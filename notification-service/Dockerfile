
# =========================
# RUNTIME STAGE
# =========================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a system group and user with ID 1000
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -D -S appuser

COPY notification-service/target/*.jar app.jar

# Change the ownership of the jar file so new user with ID 1000 can execute it
RUN chown appuser:appgroup app.jar

# disable root privileges!
USER 1000

EXPOSE 8084
EXPOSE 8184

ENTRYPOINT ["java", "-jar", "app.jar"]