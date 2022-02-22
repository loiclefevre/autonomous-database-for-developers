# Demo 11 - Always On Application
`module alwaysonapp`
- Using Oracle Universal Connection Pool (or `UCP`)
- Enabling Transparent Application Continuity
  - at the consumer group level using `DBMS_APP_CONT_ADMIN.ENABLE_TAC`
  - at the connection string level using best practices* from documentation
  - at the connection, connection pool and datasource level
- Showing that during ACID transactions nothing but a little delay happens **when the database is restarted**!
 

### See also:

- [Application Checklist for Continuous Service with Autonomous Database on Shared Infrastructure (Tech Brief - Nov. 2021)](https://www.oracle.com/a/otn/docs/checklist_atps_2021.pdf)
- [Configure Application Continuity on Autonomous Database (* documentation)](https://docs.oracle.com/en/cloud/paas/autonomous-database/adbsa/application-continuity-configure.html)
- [Client Configuration for Continuous Availability on Autonomous Database (* documentation)](https://docs.oracle.com/en/cloud/paas/autonomous-database/adbsa/application-continuity-code.html)