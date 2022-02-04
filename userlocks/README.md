# Demo 3 - Calling PL/SQL functions to manage database User Locks
`module userlocks`
- SimpleJdbcCall configuration to work with Oracle provided PL/SQL packages
- Example using the DBMS_LOCK package
    - acquiring an exclusive User Lock
    - releasing a User Lock
    - failing to get an exclusive User Lock already acquired by another session
