
CREATE TABLE permission (
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    permission_type VARCHAR(32) NOT NULL,
    granted_by VARCHAR(64) NULL,

    CONSTRAINT fk_permission_user_id FOREIGN KEY (user_id) REFERENCES user_info(id),
    CONSTRAINT fk_permission_account_id FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT fk_permission_granted_ay FOREIGN KEY (granted_by) REFERENCES user_info(id)
);