DECLARE
    username varchar2(60) := '%s'; -- filled from calling code
    password varchar2(60) := '%s'; -- filled from calling code
BEGIN
    -- Create the user for Autonomous database
    execute immediate 'create user ' || username || ' identified by "'|| password ||'" DEFAULT TABLESPACE DATA TEMPORARY TABLESPACE TEMP';

    -- Grant unlimited quota on tablespace DATA
    execute immediate 'alter user ' || username || ' quota unlimited on data';

    -- Grant Autonomous Database roles, create session, SODA API, Property Graph, Oracle Machine Learning, Alter session, Select all objects from catalog
    execute immediate 'grant dwrole, create session, soda_app, graph_developer, oml_developer, alter session, select_catalog_role, resource, connect to ' || username;

    -- Privileges to connect to Property Graph and Oracle Machine Learning GUIs
    execute immediate 'alter user ' || username || ' grant connect through GRAPH$PROXY_USER';
    execute immediate 'alter user ' || username || ' grant connect through OML$PROXY';

    -- Oracle Machine Learning for Python access
    begin
        execute immediate 'grant PYQADMIN to ' || username;
    exception when others then null;
    end;

    -- Oracle Text configuration access (Full-Text indexes)
    execute immediate 'grant execute on CTX_DDL to ' || username;

    -- Allows the user to change its database service from the SQL Database Action GUI
    execute immediate 'grant select on dba_rsrc_consumer_group_privs to ' || username;
    execute immediate 'grant execute on DBMS_SESSION to ' || username;
    execute immediate 'grant select on sys.v_$services to ' || username;

    -- Can view objects
    execute immediate 'grant select any dictionary to ' || username;

    -- To get own session statistics
    execute immediate 'grant select on sys.v_$mystat to ' || username;

    -- Automatic Indexing control
    execute immediate 'grant execute on DBMS_AUTO_INDEX to ' || username;

    -- Used for demo #3 about User Locks, grant access to the PL/SQL package
    execute immediate 'grant execute on DBMS_LOCK to ' || username;

    -- Useful for Advance Queuing
    execute immediate 'grant aq_administrator_role, aq_user_role to ' || username;
    execute immediate 'grant execute on DBMS_AQ to ' || username;

    -- Grant access to Database Actions online tools (Browsers GUI)
    ords_metadata.ords_admin.enable_schema(p_enabled => TRUE, p_schema => upper(username), p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => lower(username), p_auto_rest_auth => TRUE);
END;

/
