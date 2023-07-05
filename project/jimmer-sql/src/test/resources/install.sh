docker run \
    --restart=always \
    -d \
    --name jimmer-test-mysql \
    -p 3306:3306 \
    -e MYSQL_DATABASE=jimmer_test \
    -e MYSQL_ROOT_PASSWORD=123456 \
    mysql \
    --lower_case_table_names=1

docker restart jimmer-test-mysql

echo "WAIT mysql for 10 seconds..."
sleep 10

docker cp ./database-mysql.sql jimmer-test-mysql:/var/database-mysql.sql
docker exec jimmer-test-mysql /bin/sh -c \
    'mysql -uroot -p123456 </var/database-mysql.sql'

docker run \
    --restart=always \
    -it \
    -d \
    --name jimmer-test-postgres \
    -p 5432:5432 \
    -e POSTGRES_DB=jimmer_test \
    -e POSTGRES_USER=root \
    -e POSTGRES_PASSWORD=123456 \
    postgres

echo "WAIT postgres for 5 seconds..."
sleep 5

docker cp ./database-postgres.sql jimmer-test-postgres:/var/database-postgres.sql
docker exec jimmer-test-postgres /bin/sh -c \
    'psql -d jimmer_test </var/database-postgres.sql'