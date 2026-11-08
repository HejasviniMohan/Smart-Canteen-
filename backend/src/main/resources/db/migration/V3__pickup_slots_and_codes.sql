CREATE TABLE pickup_slots (
    slot_start TIMESTAMP PRIMARY KEY,
    capacity   INT NOT NULL CHECK (capacity >= 0),
    booked     INT NOT NULL DEFAULT 0,
    -- Last line of defence: the database itself can never hold an overbooked slot.
    CONSTRAINT chk_slot_not_overbooked CHECK (booked >= 0 AND booked <= capacity)
);

ALTER TABLE orders ADD COLUMN pickup_slot TIMESTAMP;
UPDATE orders SET pickup_slot = order_date;
ALTER TABLE orders ALTER COLUMN pickup_slot SET NOT NULL;

ALTER TABLE orders ADD COLUMN pickup_nonce VARCHAR(40);
UPDATE orders SET pickup_nonce = md5(id::text || random()::text);
ALTER TABLE orders ALTER COLUMN pickup_nonce SET NOT NULL;

ALTER TABLE orders ADD COLUMN collected_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN collected_by VARCHAR(150);

CREATE INDEX idx_orders_active_slot ON orders(status, pickup_slot);
