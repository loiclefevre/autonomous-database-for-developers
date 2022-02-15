--liquibase formatted sql

--changeset llefevre:3 dbms:oracle
-- from https://github.com/gvenzl/sample-data/tree/main/countries-cities-currencies
CREATE TABLE regions
(
    region_id     VARCHAR(2)   NOT NULL,
    name          VARCHAR(13)  NOT NULL,
    CONSTRAINT regions_pk
        PRIMARY KEY (region_id)
);

CREATE TABLE countries
(
    country_id    VARCHAR(3)     NOT NULL,
    country_code  VARCHAR(2)     NOT NULL,
    name          VARCHAR(100)   NOT NULL,
    official_name VARCHAR(200),
    population    NUMERIC(10),
    area_sq_km    NUMERIC(10,2),
    latitude      NUMERIC(8,5),
    longitude     NUMERIC(8,5),
    timezone      VARCHAR(40),
    region_id     VARCHAR(2)     NOT NULL,
    CONSTRAINT countries_pk
        PRIMARY KEY (country_id),
    CONSTRAINT countries_regions_fk001
        FOREIGN KEY (region_id) REFERENCES regions (region_id)
);

CREATE INDEX countries_regions_fk001 ON countries (region_id);
--rollback drop table countries purge;
--rollback drop table regions purge;

--changeset llefevre:4 dbms:oracle splitStatements:false
DECLARE
    l_roles     OWA.VC_ARR;
    l_modules   OWA.VC_ARR;
    l_patterns  OWA.VC_ARR;
BEGIN
    l_roles(1)   := 'SQL Developer';
    l_patterns(1) := '/*';
    ORDS.DEFINE_PRIVILEGE(
            p_privilege_name => 'rest_privilege',
            p_roles => l_roles,
            p_patterns => l_patterns,
            p_modules => l_modules,
            p_label => '',
            p_description => '',
            p_comments => NULL
        );

    ORDS.ENABLE_OBJECT(
            p_enabled => TRUE,
            p_schema  => user,
            p_object  => 'REGIONS',
            p_object_type => 'TABLE',
            p_object_alias => 'regions',
            -- DON'T DO THAT IN PRODUCTION!
            p_auto_rest_auth => FALSE
        );

    ORDS.ENABLE_OBJECT(
            p_enabled => TRUE,
            p_schema  => user,
            p_object  => 'COUNTRIES',
            p_object_type => 'TABLE',
            p_object_alias => 'countries',
            -- DON'T DO THAT IN PRODUCTION!
            p_auto_rest_auth => FALSE
        );

    COMMIT;
END;
/
--rollback BEGIN
--rollback     ORDS.ENABLE_OBJECT(
--rollback             p_enabled => FALSE,
--rollback             p_schema  => user,
--rollback             p_object  => 'REGIONS',
--rollback             p_object_type => 'TABLE'
--rollback         );
--rollback     ORDS.ENABLE_OBJECT(
--rollback             p_enabled => FALSE,
--rollback             p_schema  => user,
--rollback             p_object  => 'COUNTRIES',
--rollback             p_object_type => 'TABLE'
--rollback         );
--rollback     ORDS.DELETE_PRIVILEGE(
--rollback             p_name => 'rest_privilege'
--rollback     );
--rollback     COMMIT;
--rollback END;
--rollback /

--changeset llefevre:5 dbms:oracle splitStatements:false
BEGIN
    OAUTH.create_client(
            p_name            => 'ORDS AutoREST demo', /* OAuth client name */
            p_grant_type      => 'client_credentials',
            p_owner           => user,
            p_description     => 'A demo client for using ORDS AutoREST',
            p_support_email   => 'user@email.com',
            p_privilege_names => 'rest_privilege'
        );

    OAUTH.grant_client_role(
            p_client_name => 'ORDS AutoREST demo', /* OAuth client name (same as above) */
            p_role_name => 'SQL Developer'
        );

    COMMIT;
END;
/
--rollback BEGIN
--rollback     OAUTH.delete_client( p_name => 'ORDS AutoREST demo' );
--rollback     COMMIT;
--rollback END;
--rollback /

--changeset llefevre:6 dbms:oracle splitStatements:false
BEGIN
    ORDS.define_service(
            p_module_name    => 'Secret data!',
            p_base_path      => 'secret_data/',
            p_pattern        => 'countries/:region_name',
            p_method         => 'GET',
            p_source_type    => ORDS.source_type_collection_feed,
            p_items_per_page => 5,
            p_source         => q'[
            select c.name as country_name
              from regions r join countries c on r.region_id=c.region_id
             where r.name=:region_name
             order by 1]'
        );

    ORDS.set_module_privilege(
            p_module_name    => 'Secret data!',
            p_privilege_name => 'rest_privilege'
        );

    COMMIT;
END;
/
--rollback BEGIN
--rollback     ORDS.DELETE_MODULE(
--rollback             p_module_name => 'Secret data!'
--rollback         );
--rollback COMMIT;
--rollback END;
--rollback /
