--liquibase formatted sql
--changeset ruchira:create-user-details-table

CREATE TABLE user_details (
  id VARCHAR(64) PRIMARY KEY,
  created_at TIMESTAMP NOT NULL,
  modified_at TIMESTAMP NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  first_name VARCHAR(128) NOT NULL,
  last_name VARCHAR(256) NOT NULL,
  email VARCHAR(256) UNIQUE NOT NULL,

  CONSTRAINT fk_user_details_account_id FOREIGN KEY (account_id) REFERENCES account(id)
);