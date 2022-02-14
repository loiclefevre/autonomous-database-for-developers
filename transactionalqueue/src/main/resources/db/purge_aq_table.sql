select count(*) from aq_notifications_tbl;

DECLARE
    purge_options dbms_aqadm.aq$_purge_options_t;
BEGIN
    purge_options.block := false;
    purge_options.delivery_mode := DBMS_AQADM.PERSISTENT_OR_BUFFERED;
    DBMS_AQADM.PURGE_QUEUE_TABLE( 'AQ_NOTIFICATIONS_TBL', NULL, purge_options );
END;
/