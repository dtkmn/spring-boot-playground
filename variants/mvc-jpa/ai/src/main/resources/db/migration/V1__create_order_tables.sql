CREATE TABLE purchase_order (
    id BIGINT PRIMARY KEY,
    owner_id VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE shipment (
    order_id BIGINT PRIMARY KEY REFERENCES purchase_order (id),
    status VARCHAR(32) NOT NULL,
    delay_reason VARCHAR(1000),
    estimated_delivery DATE
);
