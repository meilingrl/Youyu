-- Additive migration for payment gateway callback verification and replay protection.
-- Safe to run on an existing `youyu` database created before 2026-05-31.

USE youyu;

ALTER TABLE payment_records
    ADD COLUMN provider_trade_no VARCHAR(100) NULL;

CREATE TABLE IF NOT EXISTS payment_callback_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gateway_code VARCHAR(50) NOT NULL,
    event_fingerprint VARCHAR(100) NOT NULL,
    payment_no VARCHAR(40) NOT NULL,
    callback_summary VARCHAR(1000),
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_callback_events_fingerprint UNIQUE (gateway_code, event_fingerprint),
    CONSTRAINT fk_payment_callback_events_payment FOREIGN KEY (payment_no) REFERENCES payment_records(payment_no)
);

CREATE INDEX idx_payment_callback_events_payment
    ON payment_callback_events(payment_no, received_at, id);
