#!/bin/bash
set -e

/opt/mssql/bin/sqlservr &
MSSQL_PID=$!

for i in {1..30}; do
  if /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'Jimmer@123456' -Q 'SELECT 1' -No 2>/dev/null; then
    break
  fi
  sleep 5
done

/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'Jimmer@123456' -Q 'CREATE DATABASE jimmer_test' -No
/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'Jimmer@123456' -d jimmer_test -i /init/database-sqlserver.sql -No

wait $MSSQL_PID
