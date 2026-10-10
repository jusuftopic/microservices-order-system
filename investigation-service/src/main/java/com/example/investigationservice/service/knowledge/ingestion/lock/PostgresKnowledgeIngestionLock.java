package com.example.investigationservice.service.knowledge.ingestion.lock;

import com.example.investigationservice.exception.KnowledgeIngestionLockException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Coordinates ingestion jobs through a PostgreSQL advisory lock.
 */
@Component
@ConditionalOnProperty(name = "app.rag.ingestion.enabled", havingValue = "true")
public class PostgresKnowledgeIngestionLock implements KnowledgeIngestionLock {

    private final JdbcTemplate jdbcTemplate;
    private final long lockId;

    public PostgresKnowledgeIngestionLock(
            JdbcTemplate jdbcTemplate,
            @Value("${app.rag.ingestion.lock-id}") long lockId
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.lockId = lockId;
    }

    @Override
    public boolean execute(Runnable ingestion) {
        try {
            return Boolean.TRUE.equals(jdbcTemplate.execute(
                    (ConnectionCallback<Boolean>) connection -> {
                        if (!tryLock(connection.prepareStatement("SELECT pg_try_advisory_lock(?)"))) {
                            return false;
                        }
                        try {
                            ingestion.run();
                            return true;
                        } finally {
                            unlock(connection.prepareStatement("SELECT pg_advisory_unlock(?)"));
                        }
                    }
            ));
        } catch (org.springframework.dao.DataAccessException exception) {
            throw new KnowledgeIngestionLockException(exception);
        }
    }

    private boolean tryLock(PreparedStatement statement) throws SQLException {
        try (statement) {
            statement.setLong(1, lockId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private void unlock(PreparedStatement statement) throws SQLException {
        try (statement) {
            statement.setLong(1, lockId);
            statement.executeQuery();
        }
    }
}
