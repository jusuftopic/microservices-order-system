package org.example.orderservice.utils.database;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Helper class checks successful/failed connection to database
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthLogger {

    private final DataSource dataSource;

    @PostConstruct
    public void logConnection() {
        try (Connection connection = dataSource.getConnection()) {
            log.info("[ORDER-SERVICE][DATABASE] Database connected successfully: {}",
                    connection.getMetaData().getURL());
        } catch (Exception e) {
            log.error("[ORDER-SERVICE][DATABASE] Database connection FAILED", e);
        }
    }
}
