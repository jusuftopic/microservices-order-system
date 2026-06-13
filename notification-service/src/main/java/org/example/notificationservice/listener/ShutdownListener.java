package org.example.notificationservice.listener;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Component listens on graceful shutdown signals for the service
 */
@Component
@Slf4j
public class ShutdownListener {

    @PreDestroy
    public void onShutdown() {
        log.info("[NOTIFICATION-SERVICE] Shutting down gracefully. No new work will be accepted.");
    }
}
