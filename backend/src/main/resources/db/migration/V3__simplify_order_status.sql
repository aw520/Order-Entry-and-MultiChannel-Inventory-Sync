-- P0 only records accepted orders. Warehouse lifecycle states are out of scope.
UPDATE customer_order
SET status = 'ALLOCATED'
WHERE status <> 'ALLOCATED';

ALTER TABLE customer_order
    DROP CHECK chk_order_status,
    ADD CONSTRAINT chk_order_status CHECK (status = 'ALLOCATED');
