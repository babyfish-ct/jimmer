#!/bin/bash
set -e

echo "Running database-oracle.sql in FREEPDB1..."
sqlplus -s system/"$ORACLE_PASSWORD"@localhost/FREEPDB1 @/init/database-oracle.sql
echo "Oracle initialization completed."
