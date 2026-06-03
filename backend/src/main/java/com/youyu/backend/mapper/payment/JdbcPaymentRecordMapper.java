package com.youyu.backend.mapper.payment;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcPaymentRecordMapper implements PaymentRecordMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPaymentRecordMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, Object> findByPaymentNo(String paymentNo) {
        return jdbcTemplate.query("""
                        SELECT id, order_id, payment_no, payment_method, payment_channel, payment_status,
                               amount, initiated_at, succeeded_at, failed_reason, callback_summary,
                               provider_trade_no
                        FROM payment_records
                        WHERE payment_no = ?
                        """,
                (rs, rowNum) -> {
                    Map<String, Object> payment = new LinkedHashMap<>();
                    payment.put("id", rs.getLong("id"));
                    payment.put("orderId", rs.getLong("order_id"));
                    payment.put("paymentNo", rs.getString("payment_no"));
                    payment.put("paymentMethod", rs.getString("payment_method"));
                    payment.put("paymentChannel", rs.getString("payment_channel"));
                    payment.put("paymentStatus", rs.getString("payment_status"));
                    payment.put("paymentAmount", rs.getBigDecimal("amount"));
                    payment.put("initiatedAt", toLocalDateTime(rs.getTimestamp("initiated_at")));
                    payment.put("paidAt", toLocalDateTime(rs.getTimestamp("succeeded_at")));
                    payment.put("failedReason", rs.getString("failed_reason"));
                    payment.put("callbackSummary", rs.getString("callback_summary"));
                    payment.put("providerTradeNo", rs.getString("provider_trade_no"));
                    return payment;
                },
                paymentNo).stream().findFirst().orElse(null);
    }

    @Override
    public boolean recordCallback(String gatewayCode, String eventFingerprint, String paymentNo, String callbackSummary) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO payment_callback_events (
                                gateway_code, event_fingerprint, payment_no, callback_summary, received_at
                            ) VALUES (?, ?, ?, ?, ?)
                            """,
                    gatewayCode, eventFingerprint, paymentNo, callbackSummary, Timestamp.valueOf(LocalDateTime.now()));
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public void updateStatus(String paymentNo, String paymentStatus, String providerTradeNo,
                             String failedReason, String callbackSummary, LocalDateTime succeededAt) {
        jdbcTemplate.update("""
                        UPDATE payment_records
                        SET payment_status = ?, provider_trade_no = ?, failed_reason = ?,
                            callback_summary = ?, succeeded_at = ?
                        WHERE payment_no = ?
                        """,
                paymentStatus, providerTradeNo, failedReason, callbackSummary,
                succeededAt == null ? null : Timestamp.valueOf(succeededAt), paymentNo);
    }

    @Override
    public int markTimedOutPayments(Long orderId, LocalDateTime initiatedBefore) {
        return jdbcTemplate.update("""
                        UPDATE payment_records
                        SET payment_status = 'timed_out', failed_reason = 'Payment attempt timed out'
                        WHERE order_id = ? AND payment_status = 'initiated' AND initiated_at < ?
                        """,
                orderId, Timestamp.valueOf(initiatedBefore));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
