-- Online orders wait in PENDING_PAYMENT and stay hidden from the kitchen until paid.
ALTER TABLE orders DROP CONSTRAINT orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (status IN
    ('PENDING_PAYMENT','PLACED','ACCEPTED','PREPARING','READY','COMPLETED','CANCELLED'));

ALTER TABLE orders ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'CASH_ON_PICKUP'
    CHECK (payment_method IN ('ONLINE','CASH_ON_PICKUP'));
ALTER TABLE orders ALTER COLUMN payment_method DROP DEFAULT;

-- Optimistic locking: two staff members cannot silently overwrite each other.
ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- A retried or double-clicked checkout returns the original order.
ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(80);
CREATE UNIQUE INDEX uk_orders_user_idempotency ON orders(user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Tokens restart daily, so they are unique per business date rather than forever.
ALTER TABLE orders ADD COLUMN business_date DATE;
UPDATE orders SET business_date = CAST(order_date AS DATE);
ALTER TABLE orders ALTER COLUMN business_date SET NOT NULL;
ALTER TABLE orders DROP CONSTRAINT orders_token_number_key;
ALTER TABLE orders ADD CONSTRAINT uk_orders_date_token UNIQUE (business_date, token_number);

CREATE TABLE daily_token_counters (
    business_date DATE PRIMARY KEY,
    last_value    INT NOT NULL
);

-- Foods with order history are hidden, never physically deleted.
ALTER TABLE food_items ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE payments ADD COLUMN gateway VARCHAR(20) NOT NULL DEFAULT 'razorpay';
ALTER TABLE payments ALTER COLUMN gateway DROP DEFAULT;
ALTER TABLE payments ADD COLUMN refunded_at TIMESTAMP;
CREATE INDEX idx_payments_gateway_order ON payments(razorpay_order_id);
CREATE INDEX idx_payments_status ON payments(payment_status);

-- Gateways can deliver a webhook more than once; each event is processed only once.
CREATE TABLE payment_events (
    event_id    VARCHAR(120) PRIMARY KEY,
    event_type  VARCHAR(60)  NOT NULL,
    received_at TIMESTAMP    NOT NULL
);
