with my_stats as (SELECT id,
                         row_created_on,
                         row_created_on - lag(row_created_on) over (order by row_created_on) as delta_time,
                         data
                  FROM
                      always_on)
select *
from my_stats
WHERE
        delta_time > interval '5' second;
