--liquibase formatted sql

--changeset llefevre:9 dbms:oracle
create table devices_relational (
    id_machine varchar2(50) not null primary key,
    country varchar2(100) not null,
    state varchar2(100) not null,
    city varchar2(100) not null,
    geometry sdo_geometry not null
);
--rollback drop table devices_relational purge;
