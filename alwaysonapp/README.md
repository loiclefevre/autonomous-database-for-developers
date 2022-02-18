# Demo 11 - Always On Application
`module alwaysonapp`
- Using Oracle Universal Connection Pool (or `UCP`)
- Enabling Transparent Application Continuity
  - at the consumer group level using `DBMS_APP_CONT_ADMIN.ENABLE_TAC`
  - at the connection string level using best practices* from documentation
  - at the connection, connection pool and datasource level
- Showing that during ACID transactions nothing but a little delay happens **when the database is restarted**!
 

### See also:

- [Configure Application Continuity on Autonomous Database (* documentation)](https://docs.oracle.com/en/cloud/paas/autonomous-database/adbsa/application-continuity-configure.html)