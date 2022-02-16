--liquibase formatted sql

--changeset llefevre:7 dbms:oracle
CREATE TABLE country_polygons (
      country_id    VARCHAR(3)     NOT NULL,
      geometry      CLOB           NOT NULL, -- using CLOB instead of BLOB because of SDO_UTIL.FROM_GEOJSON()
      CONSTRAINT country_polygons_pk
          PRIMARY KEY (country_id)
);

ALTER TABLE country_polygons ADD CONSTRAINT country_polygons_geometry_is_json check (geometry is json);
--rollback drop table country_polygons purge;

--changeset llefevre:8 dbms:oracle
CREATE INDEX spatial_idx ON country_polygons (json_value( geometry, '$' RETURNING SDO_GEOMETRY ERROR ON ERROR NULL ON EMPTY))
    INDEXTYPE IS MDSYS.SPATIAL_INDEX_V2;
--rollback drop index spatial_idx;
