package com.example.demo3.locks;

public interface Locks {
    // Results for DBMS_LOCK function calls
	int Success=0;
	int Timeout=1;
	int Deadlock=2;
	int ParameterError=3;
	int AlreadyOwning=4; // for request
	int NotOwning=4; // for release
	int IllegalLockHandle=5;

	// Expiration for identifier allocation (name -> lock ID): 10 days
	int AllocationExpirationSecs = 864000;

	// Lock acquisition modes
	int NullMode = 1;
	int SubSharedMode = 2;
	int SubExclusiveMode = 3;
	int SharedMode = 4;
	int SharedSubExclusiveMode = 5;
	int ExclusiveMode = 6;

}
