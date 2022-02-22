-- In the case you dropped the table "inadvertently" (drop table my_table without purge),
-- you can recover it from the recycle bin (if the recycle bin has not been disabled)
flashback table my_table to before drop;
