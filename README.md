# Autonomous Database for Developers

Repository providing various demos using an Oracle Autonomous Database. Each demo is a Maven module depending upon the `common` module. 

## Setup
You'll need to set several environment variables to be able to run these examples:
- region: the OCI region where the Autonomous Database has been provisioned
- user: the username you want to connect to the Autonomous Database 
  (this will be the username that you'll create during **Demo 1** and use later on)
- password: the password to connect to the Autonomous Database
- database: the database name you want to connect to (unique name, see your tnsnames.ora inside the wallet)

#### Example
- `export region=eu-marseille-1` (see [here for a list](https://docs.cloud.oracle.com/en-us/iaas/Content/General/Concepts/regions.htm#top))
- `export user=bob`
- `export password=5uper_Pa55w0rd` set this as the same as the ADMIN user (see [here for password complexity 
  requirements](https://docs.oracle.com/en/cloud/paas/autonomous-database/adbsa/manage-users-create.html#GUID-72DFAF2A-C4C3-4FAC-A75B-846CC6EDBA3F))
- `export database=iq1ffzid3wfss2e_myatps`

## Demo 1 - Using the REST Enabled SQL Service to create a new database user
`module sqlviarest`
- WebClient synchronous call to the REST Enabled SQL Service powered by ORDS
- Basic Authentication using the ADMIN user account of the database
- JSON response mapping to a POJO

## Demo 2 - Connecting using Spring Boot JDBC
`module connecting`
- Using Oracle Easy Connect (EZCONNECT) passing JDBC Connection related arguments in the Connection string
- Using jdbcTemplate to get the configured Row Fetch Size
- Hikari connection pool
- Liquibase changelog to create a table if it doesn't exist

## Demo 3 - Calling PL/SQL functions to manage database User Locks 
`module userlocks`
- SimpleJdbcCall configuration to work with Oracle provided PL/SQL packages
- Example using the DBMS_LOCK package
  - acquiring an exclusive User Lock
  - releasing a User Lock
  - failing to get an exclusive User Lock already acquired by another session

## Demo 4 - Generating AWR and ADDM reports in order to analyze performance
## Demo 5 - Enabling Automatic Index reporting
## Demo 6 - Distinguishing the service names to use, what and when? 
