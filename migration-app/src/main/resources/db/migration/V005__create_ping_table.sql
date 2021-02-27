
CREATE TABLE ping (
    id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    uri VARCHAR(512) NOT NULL,
    method VARCHAR(10) NOT NULL,
    frequency INT NOT NULL,

    CONSTRAINT fk_ping_account_id FOREIGN KEY (account_id) REFERENCES account(id),
    PRIMARY KEY(id)
);