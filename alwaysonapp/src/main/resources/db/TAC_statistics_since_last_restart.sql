-- Run as ADMIN
select stats.*,
       stats."Protected calls"*100/NULLIF(stats."Calls in requests",0) as "Protected %"
  from (
         select name, value
         from gv$sysstat
         where name in (
                'logons cumulative',
                'user calls', 'user commits',
                'user rollbacks',
                -- Transparent Application Continuity statistics
                'cumulative begin requests',
                'cumulative user calls in requests',
                'cumulative user calls protected by Application Continuity')
       )
 pivot (
    sum(value)
    for name in (
        'logons cumulative' as "Logons",
        'user calls' as "User calls",
        'user commits' as "Commits",
        'user rollbacks' as "Rollbacks",
        'cumulative begin requests' as "Requests",
        'cumulative user calls in requests' as "Calls in requests",
        'cumulative user calls protected by Application Continuity' as "Protected calls"
     )
 ) stats;