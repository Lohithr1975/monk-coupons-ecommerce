INSERT INTO coupons (id, type, details_json, active, created_at) VALUES
('cart10p','cart-wise','{"threshold":100,"percent":10}',true,CURRENT_TIMESTAMP()),
('prod20p','product-wise','{"product_id":"1","percent":20}',true,CURRENT_TIMESTAMP()),
('b2g1-example','bxgy','{"buy_products":[{"product_id":"1","quantity":2}],"get_products":[{"product_id":"3","quantity":1}],"repetition_limit":3}',true,CURRENT_TIMESTAMP());
