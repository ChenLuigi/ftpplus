select * from pcuserauto where pcName in (select pcName from pcuser) order by pcName asc limit 203