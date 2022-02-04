--liquibase formatted sql

--changeset llefevre:1 dbms:oracle
-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-TABLE.html
create table my_table (
    id number  generated as identity (cache 100) primary key, -- auto generated identifier
    first_name varchar2(100) not null,
    last_name  varchar2(100) not null
);

insert into my_table (first_name, last_name) values ('Loïc', 'Lefèvre');
commit;
--rollback drop table my_table purge;
