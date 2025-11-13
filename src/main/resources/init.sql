CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE file_entity (
                             id SERIAL PRIMARY KEY,
                             filename VARCHAR(255) NOT NULL,
                             created_at TIMESTAMP,
                             size BIGINT CHECK (size >= 0),
    location VARCHAR(255) NOT NULL,
    owner_id BIGINT NOT NULL,
    file_type VARCHAR(255),
    is_archived BOOLEAN DEFAULT false,
    checksum VARCHAR(255),
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE operation (
                           id SERIAL PRIMARY KEY,
                           timestamp TIMESTAMP,
                           operation_type VARCHAR(50) NOT NULL,
                           file_id BIGINT,
                           user_id BIGINT NOT NULL,
                           details VARCHAR(1000),
                           FOREIGN KEY (file_id) REFERENCES file_entity(id) ON DELETE SET NULL,
                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);