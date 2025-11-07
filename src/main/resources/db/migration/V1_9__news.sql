CREATE TABLE news (
                      id BIGSERIAL PRIMARY KEY,
                      title VARCHAR(255) NOT NULL,
                      content TEXT NOT NULL,
                      image_url TEXT,
                      created_at TIMESTAMPTZ DEFAULT now(),
                      source VARCHAR(255),
                      is_featured BOOLEAN DEFAULT FALSE
);
