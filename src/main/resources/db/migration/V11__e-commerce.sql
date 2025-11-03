CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          movie_id BIGINT REFERENCES movies(id) ON DELETE SET NULL,
                          name VARCHAR(255),
                          price DECIMAL(10,2),
                          image_url TEXT,
                          stock INT DEFAULT 0,
                          created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
                        total DECIMAL(10,2),
                        status VARCHAR(32) DEFAULT 'CREATED',
                        created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE order_items (
                             order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
                             product_id BIGINT REFERENCES products(id),
                             quantity INT,
                             price DECIMAL(10,2),
                             PRIMARY KEY (order_id, product_id)
);
