--liquibase formatted sql

--changeset llefevre:2 dbms:oracle
BEGIN
    DBMS_AQADM.CREATE_QUEUE_TABLE(
            queue_table        =>  'AQ_NOTIFICATIONS_TBL',
            queue_payload_type =>  'RAW');

    DBMS_AQADM.CREATE_QUEUE(
            queue_name         =>  'AQ_NOTIFICATIONS_QUEUE',
            queue_table        =>  'AQ_NOTIFICATIONS_TBL');

    DBMS_AQADM.START_QUEUE(
            queue_name         => 'AQ_NOTIFICATIONS_QUEUE');

    commit;
END;
/
--rollback BEGIN
--rollback     DBMS_AQADM.STOP_QUEUE( queue_name =>         'AQ_NOTIFICATIONS_QUEUE');
--rollback     DBMS_AQADM.DROP_QUEUE( queue_name =>         'AQ_NOTIFICATIONS_QUEUE');
--rollback     DBMS_AQADM.DROP_QUEUE_TABLE( queue_table =>  'AQ_NOTIFICATIONS_TBL');
--rollback     commit;
--rollback END;
--rollback /

