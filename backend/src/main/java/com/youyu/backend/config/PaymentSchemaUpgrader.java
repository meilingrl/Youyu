package com.youyu.backend.config;

import java.sql.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies additive payment schema upgrades for pre-existing MySQL databases.
 * {@code schema.sql} only creates missing tables, so older local databases keep
 * their legacy shape until these additive fixes run.
 */
@Component
public class PaymentSchemaUpgrader {

    private static final Logger log = LoggerFactory.getLogger(PaymentSchemaUpgrader.class);

    private final JdbcTemplate jdbcTemplate;

    public PaymentSchemaUpgrader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upgradeIfNeeded() {
        ensurePaymentRecordColumns();
        ensurePaymentCallbackEventsTable();
        ensureRefundColumns();
    }

    private void ensurePaymentRecordColumns() {
        addColumnIfMissing("payment_records", "provider_trade_no", "VARCHAR(100) NULL");
    }

    private void ensurePaymentCallbackEventsTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS payment_callback_events (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    gateway_code VARCHAR(50) NOT NULL,
                    event_fingerprint VARCHAR(100) NOT NULL,
                    payment_no VARCHAR(40) NOT NULL,
                    callback_summary VARCHAR(1000),
                    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_payment_callback_events_fingerprint UNIQUE (gateway_code, event_fingerprint),
                    CONSTRAINT fk_payment_callback_events_payment FOREIGN KEY (payment_no) REFERENCES payment_records(payment_no)
                )
                """);
        if (usesMySqlCompatibleCatalog()) {
            ensureIndex("payment_callback_events", "idx_payment_callback_events_payment",
                    "payment_no, received_at, id");
        }
    }

    private void ensureRefundColumns() {
        addColumnIfMissing("refund_records", "gateway_response_summary", "VARCHAR(1000) NULL");
        addColumnIfMissing("refund_records", "failed_reason", "VARCHAR(500) NULL");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        log.info("Added {}.{}", tableName, columnName);
    }

    private void ensureIndex(String tableName, String indexName, String columns) {
        if (indexExists(tableName, indexName)) {
            return;
        }
        try {
            jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + tableName + " (" + columns + ")");
            log.info("Added index {} on {}", indexName, tableName);
        } catch (Exception ex) {
            log.warn("Could not add index {} on {}: {}", indexName, tableName, ex.getMessage());
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) conn -> {
            try (ResultSet upper = conn.getMetaData().getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
                if (upper.next()) {
                    return true;
                }
            }
            try (ResultSet lower = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
                return lower.next();
            }
        }));
    }

    private boolean indexExists(String tableName, String indexName) {
        try {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                      AND INDEX_NAME = ?
                    """, Long.class, tableName, indexName);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean usesMySqlCompatibleCatalog() {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) conn -> {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product == null) {
                return false;
            }
            String normalized = product.toLowerCase();
            return normalized.contains("mysql") || normalized.contains("mariadb");
        }));
    }
}
