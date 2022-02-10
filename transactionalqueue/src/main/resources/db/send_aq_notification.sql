DECLARE
    enqueue_options     dbms_aq.enqueue_options_t;
    message_properties  dbms_aq.message_properties_t;
    message_handle      RAW(16);
    message             VARCHAR2(32000);
BEGIN
    message :=  UTL_RAW.CAST_TO_RAW('{"notification": "new message from PL/SQL"}');
    DBMS_AQ.ENQUEUE(queue_name => 'AQ_NOTIFICATIONS_QUEUE',
                    enqueue_options    => enqueue_options,
                    message_properties => message_properties,
                    payload  => message,
                    msgid   => message_handle);
    COMMIT;
END;

/
