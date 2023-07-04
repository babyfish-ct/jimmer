docker run \
	-it \
        -d \
        --restart=always \
	--name jimmer-demo-postgres \
        --network jimmer-demo-network \
	-p 5001:5432 \
	-e POSTGRES_DB=jimmer_demo \
	-e POSTGRES_USER=root \
        -e POSTGRES_PASSWORD=123456 \
        postgres
echo "WAIT postgres for 5 seconds"
sleep 5
docker cp ./jimmer-demo.sql jimmer-demo-postgres:/var/jimmer-demo.sql
docker exec jimmer-demo-postgres /bin/sh -c \
	'psql -d jimmer_demo </var/jimmer-demo.sql'

docker run \
	-it \
	-d \
	--restart=always \
	--name debezium-connect \
	--network jimmer-demo-network \
	-p 5002:8083 \
        -e BOOTSTRAP_SERVERS=jimmer-demo-kafka:9092 \
	-e GROUP_ID=1 \
	-e CONFIG_STORAGE_TOPIC=my_connect_configs \
	-e OFFSET_STORAGE_TOPIC=my_connect_offsets \
	-e STATUS_STORAGE_TOPIC=my_connect_statuses \
	debezium/connect

curl \
	-i \
	-X POST \
	-H "Accept:application/json" \
	-H "Content-Type:application/json" \
	localhost:5002/connectors/ -d \
	'{
 		"name": "inventory-connector",  
		"config": {  
   			"connector.class": "io.debezium.connector.postgresql.PostgresConnector",
 			"tasks.max": "1",  
 			"database.hostname": "jimmer-demo-postgres",  
 			"database.port": "5432",
 			"database.user": "root",
			"database.password": "123456",
                        "database.server.id": "watched-db",
			"database.dbname": "jimmer_demo",  
                        "topic.prefix": "debezium",  
                        "table.include.list": "public.book_store,public.book,public.author,public.book_author_mapping,public.tree_node",  
                        "schema.history.internal.kafka.bootstrap.servers": "jimmer-demo-kafka:9092",  
                        "schema.history.internal.kafka.topic": "schema-changes.jimmer_demo"  
		}
	}'
