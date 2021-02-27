
CREATE TABLE ping_header (
    created_at TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    ping_id VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    value VARCHAR(512) NOT NULL,

    CONSTRAINT fk_ping_header_ping_id FOREIGN KEY (ping_id) REFERENCES ping(id)
);

CREATE INDEX ping_id_idx_ping_header ON ping_header(ping_id);