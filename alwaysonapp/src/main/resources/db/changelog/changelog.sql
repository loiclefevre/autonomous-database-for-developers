--liquibase formatted sql

--changeset llefevre:11 dbms:oracle
create table always_on (
      id number generated as identity (cache 100) primary key, -- auto generated identifier
      row_created_on TIMESTAMP(6) default systimestamp,
      data VARCHAR2(100) not null
);
--rollback drop table always_on purge;
