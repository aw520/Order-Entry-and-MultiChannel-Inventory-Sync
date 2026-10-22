INSERT INTO sku (sku, name) VALUES
                                ('TShirt-White', 'White T-Shirt'),
                                ('TShirt-Black', 'Black T-Shirt');

INSERT INTO facility (code, name) VALUES
                                      ('LA-DC', 'Los Angeles Distribution Center'),
                                      ('NJ-DC', 'New Jersey Distribution Center');

-- These are the PDF's PHYSICAL on-hand quantities.
-- Reservations and channel holds are added below, without increasing on-hand.
INSERT INTO inventory
(sku, facility, on_hand_qty, reserved_qty,
 amazon_hold_qty, walmart_hold_qty)
VALUES
    ('TShirt-White', 'LA-DC', 10, 7, 1, 1),
    ('TShirt-White', 'NJ-DC',  3, 3, 0, 0),
    ('TShirt-Black', 'LA-DC',  0, 0, 0, 0),
    ('TShirt-Black', 'NJ-DC',  9, 3, 2, 2);

-- Exactly 10 open White orders, each quantity=1.
-- 7 from LA; 3 from NJ. All remain unfulfilled and reserved.
INSERT INTO customer_order
(sku, facility, channel, quantity, status, is_demo)
VALUES
    ('TShirt-White','LA-DC','HISTORICAL',1,'ALLOCATED',TRUE),
    ('TShirt-White','LA-DC','HISTORICAL',1,'ALLOCATED',TRUE),
    ('TShirt-White','LA-DC','HISTORICAL',1,'ALLOCATED',TRUE),
    ('TShirt-White','LA-DC','HISTORICAL',1,'PICKING',TRUE),
    ('TShirt-White','LA-DC','HISTORICAL',1,'PICKING',TRUE),
    ('TShirt-White','LA-DC','HISTORICAL',1,'PACKED',TRUE),
    ('TShirt-White','LA-DC','HISTORICAL',1,'PACKED',TRUE),
    ('TShirt-White','NJ-DC','HISTORICAL',1,'ALLOCATED',TRUE),
    ('TShirt-White','NJ-DC','HISTORICAL',1,'PICKING',TRUE),
    ('TShirt-White','NJ-DC','HISTORICAL',1,'PACKED',TRUE);

-- Exactly 3 open Black orders, each quantity=1, all at NJ.
INSERT INTO customer_order
(sku, facility, channel, quantity, status, is_demo)
VALUES
    ('TShirt-Black','NJ-DC','HISTORICAL',1,'ALLOCATED',TRUE),
    ('TShirt-Black','NJ-DC','HISTORICAL',1,'PICKING',TRUE),
    ('TShirt-Black','NJ-DC','HISTORICAL',1,'PACKED',TRUE);