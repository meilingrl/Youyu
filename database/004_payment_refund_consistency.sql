-- Additive migration for gateway-backed refund execution diagnostics.
-- Safe to run after 003_payment_gateway_foundation.sql.

USE youyu;

ALTER TABLE refund_records
    ADD COLUMN gateway_response_summary VARCHAR(1000) NULL,
    ADD COLUMN failed_reason VARCHAR(500) NULL;
