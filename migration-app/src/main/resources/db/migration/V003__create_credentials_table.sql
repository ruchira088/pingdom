
CREATE TABLE credentials (
    user_id VARCHAR(64) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    salted_password_hash VARCHAR(128) NOT NULL,

    CONSTRAINT fk_credentials_user_id FOREIGN KEY (user_id) REFERENCES user_info(id)
);