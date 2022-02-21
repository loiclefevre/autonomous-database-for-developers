# Demo 3 - Calling PL/SQL functions to manage database User Locks
`module userlocks`
- `SimpleJdbcCall` configuration to work with Oracle provided PL/SQL packages
- Example using the DBMS_LOCK package
    - acquiring an exclusive User Lock
    - releasing a User Lock
    - failing to get an exclusive User Lock already acquired by another session

## Goal for this demo
Highlight how to build the `SimpleJdbcCall` Spring JDBC objects when the PL/SQL package is not owned by you but by the database (e.g. inside the SYS schema).

The demo invokes a stored procedure that has an output parameter, and we rely on the dictionary (Metadata) analysis at startup time to check for the name and data types of the parameters.

We also invoke 2 stored functions that have multiple implementations (polymorphism) which forces us to use the `.withoutProcedureColumnMetaDataAccess()` Spring JDBC method to not rely on the database dictionary but rather we force the data types and parameter names by using the documentation.

Regarding the support of the PL/SQL BOOLEAN data type, you'll see that we use `java.sql.Types.OTHER`. 
