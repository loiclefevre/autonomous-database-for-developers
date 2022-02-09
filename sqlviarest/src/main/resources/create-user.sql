DECLARE
username varchar2(60) := '%s';
							password varchar2(60) := '%s';
BEGIN
execute immediate 'create user ' || username || ' identified by "'|| password ||'" DEFAULT TABLESPACE DATA TEMPORARY TABLESPACE TEMP';
execute immediate 'alter user ' || username || ' quota unlimited on data';
execute immediate 'grant dwrole, create session, soda_app, graph_developer, oml_developer, alter session, select_catalog_role to ' || username;
execute immediate 'alter user ' || username || ' grant connect through GRAPH$PROXY_USER';
execute immediate 'alter user ' || username || ' grant connect through OML$PROXY';

begin
execute immediate 'grant PYQADMIN to ' || username;
exception when others then null;
end;

execute immediate 'grant execute on CTX_DDL to ' || username;
execute immediate 'grant select on dba_rsrc_consumer_group_privs to ' || username;
execute immediate 'grant execute on DBMS_SESSION to ' || username;
execute immediate 'grant select on sys.v_$services to ' || username;
execute immediate 'grant select any dictionary to ' || username;
execute immediate 'grant execute on DBMS_AUTO_INDEX to ' || username;
execute immediate 'grant execute on DBMS_LOCK to ' || username; -- used for demo3

ords_admin.enable_schema(p_enabled => TRUE, p_schema => upper(username), p_url_mapping_type => 'BASE_PATH', p_url_mapping_pattern => lower(username), p_auto_rest_auth => TRUE);
END;

/