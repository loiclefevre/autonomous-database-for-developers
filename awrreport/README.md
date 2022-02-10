# Demo 5 - Generating AWR and ADDM reports in order to analyze performance
`module awrreport`
- Using 2 Hikari connection pools
  - with the "developer" user (as primary) connected with the **HIGH** database service name 
  - with the ADMIN user to take initial and final snapshots and then generate the report  
- Using the provided dataset SSSB ([sample star schema benchmark](https://docs.oracle.
  com/en/cloud/paas/autonomous-database/adbsa/sample-queries.html)) to simulate some workload
- Using Spring `JdbcTemplate`, `NamedParameterJdbcTemplate`, `SqlParameterSource`, and `RowMapper`  
