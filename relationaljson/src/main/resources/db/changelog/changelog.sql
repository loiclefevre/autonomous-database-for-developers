--liquibase formatted sql

--changeset llefevre:9 dbms:oracle
CREATE TABLE devices (
    id VARCHAR2(255) default SYS_GUID() NOT NULL PRIMARY KEY,
    created_on TIMESTAMP(6) default sys_extract_utc(SYSTIMESTAMP),
    json_document BLOB NOT NULL
);

ALTER TABLE devices ADD CONSTRAINT devices_json_document_is_json check (json_document is json);
--rollback drop table devices purge;
