--liquibase formatted sql
--changeset ruchira:create-account-table

CREATE TABLE account (
    id VARCHAR(64) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    name VARCHAR(128) NOT NULL
);