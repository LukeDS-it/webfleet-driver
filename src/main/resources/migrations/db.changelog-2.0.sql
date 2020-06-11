-- liquibase formatted sql

-- changeset luca:rename-tag
alter table offset_store rename column tag to consumer_name;