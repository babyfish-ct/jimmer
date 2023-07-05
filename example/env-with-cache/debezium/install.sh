docker run \
    --restart=always \
    -d \
    --name debezium-demo-zookeeper \
    -p 5000:2181 \
    -v /etc/localtime:/etc/localtime \
    zookeeper

docker run \
    --restart=always \
    -d \
    --name debezium-demo-kafka \
    --hostname kafka \
    -p 5100:5100 \
    -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
    -e KAFKA_ADVERTISED_LISTENERS=INSIDE://kafka:9092,OUTSIDE://localhost:5100 \
    -e KAFKA_LISTENERS=INSIDE://:9092,OUTSIDE://:5100 \
    -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=INSIDE:PLAINTEXT,OUTSIDE:PLAINTEXT \
    -e KAFKA_INTER_BROKER_LISTENER_NAME=INSIDE \
    --link debezium-demo-zookeeper:zookeeper \
    wurstmeister/kafka

docker run \
    --restart=always \
    -it \
    -d \
    --name debezium-demo-kafdrop \
    -p 5101:9000 \
    -e KAFKA_BROKERCONNECT=kafka:9092 \
    --link debezium-demo-kafka:kafka \
    linuxforhealth/kafdrop

docker run \
    --restart=always \
    -it \
    -d \
    --name debezium-demo-postgres \
    -p 5200:5432 \
    -e POSTGRES_DB=jimmer_demo \
    -e POSTGRES_USER=root \
    -e POSTGRES_PASSWORD=123456 \
    postgres

echo "WAIT postgres for 5 seconds..."
sleep 5

docker cp \
    ./jimmer-demo.sql \
    debezium-demo-postgres:/var/jimmer-demo.sql
docker exec \
    debezium-demo-postgres /bin/sh -c \
    'psql -d jimmer_demo </var/jimmer-demo.sql'
docker restart debezium-demo-postgres

docker run \
    --restart=always \
    -it \
    -d \
    --name debezium-demo-connect \
    -p 5300:8083 \
    -e GROUP_ID=1 \
    -e CONFIG_STORAGE_TOPIC=debezium_connect_configs \
    -e OFFSET_STORAGE_TOPIC=debezium_connect_offsets \
    -e STATUS_STORAGE_TOPIC=debezimu_connect_statuses \
    -e CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE=false \
    -e BOOTSTRAP_SERVERS=kafka:9092 \
    --link debezium-demo-kafka:kafka \
    --link debezium-demo-postgres:postgres \
    debezium/connect

docker run \
	--restart=always \
	-d \
	-it \
	--name debezium-demo-redis \
	-p 5400:6379 \
	redis

echo "WAIT kafka-connector for 10 seconds..."
sleep 10

curl \
    -i \
    -X POST \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    localhost:5300/connectors/ \
    -d '@connector.json'
