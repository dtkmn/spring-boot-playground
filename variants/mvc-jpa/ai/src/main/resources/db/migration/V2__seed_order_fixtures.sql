-- Synthetic records for the disposable local pilot only.
INSERT INTO purchase_order (id, owner_id, status) VALUES
    (1001, 'caller-a', 'DELIVERED'),
    (1002, 'caller-a', 'SHIPPED'),
    (2001, 'caller-b', 'SHIPPED');

INSERT INTO shipment (order_id, status, delay_reason, estimated_delivery) VALUES
    (1001, 'DELIVERED', NULL, NULL),
    (1002, 'DELAYED', 'Carrier backlog', NULL),
    (2001, 'DELAYED', 'Private B shipment reason', NULL);
