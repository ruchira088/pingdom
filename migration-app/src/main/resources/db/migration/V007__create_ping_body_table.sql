
CREATE TABLE ping_body (
    ping_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    body VARCHAR(2048) NULL,

    CONSTRAINT fk_ping_body_ping_id FOREIGN KEY (ping_id) REFERENCES ping(id),
    PRIMARY KEY (ping_id)
);