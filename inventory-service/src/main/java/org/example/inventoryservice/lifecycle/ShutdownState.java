package org.example.inventoryservice.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks whether application shutdown has started.
 *
 * Components that initiate background or external work can use this state
 * to avoid starting new operations during shutdown.
 */
@Component
@Slf4j
public class ShutdownState implements ApplicationListener<ContextClosedEvent>
{

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (shuttingDown.compareAndSet(false, true)) {
            log.info(
                    "[INVENTORY-SERVICE][SHUTDOWN-STATE] Graceful shutdown initiated. "
                            + "New background work will no longer be started."
            );
        }
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }
}
