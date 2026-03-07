CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL
);

INSERT INTO customer (first_name, last_name)
VALUES ('John', 'Doe');
