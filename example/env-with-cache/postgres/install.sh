docker run \
	-it \
	-d \
	--name jimmer-demo2-zookeeper \
	-p 5001:2181 \
	-p 5002:2888 \
	-p 5003:3888 \
	debezium/zookeeper

docker run \
	-it \
	-d \
        --name jimmer-demo2-kafka \
        --hostname kafka \
        -p 5100:9092 \
        --link jimmer-demo2-zookeeper:zookeeper \
        debezium/kafka

docker run \
    -it \
    -d \
    --name jimmer-demo2-kafdrop \
    -p 5101:9000 \
    -e KAFKA_BROKERCONNECT=kafka:9092 \
    --link jimmer-demo2-kafka:kafka \
    linuxforhealth/kafdrop

docker run \
	-it \
        -d \
	--name jimmer-demo2-db \
	-p 5200:5432 \
	-e POSTGRES_DB=jimmer_demo \
	-e POSTGRES_USER=root \
        -e POSTGRES_PASSWORD=123456 \
        postgres
echo "WAIT postgres for 5 seconds"
sleep 5
docker cp ./jimmer-demo.sql jimmer-demo2-db:/var/jimmer-demo.sql
docker exec jimmer-demo2-db /bin/sh -c \
	'psql -d jimmer_demo </var/jimmer-demo.sql'

docker run \
	-it \
        -d \
	--name jimmer-demo2-connect \
        -p 5300:8083 \
        -e GROUP_ID=1 \
        -e CONFIG_STORAGE_TOPIC=debezium_connect_configs \
        -e OFFSET_STORAGE_TOPIC=debezium_connect_offsets \
        -e STATUS_STORAGE_TOPIC=debezimu_connect_statuses \
	-e CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE=false \
        --link jimmer-demo2-kafka:kafka \
        --link jimmer-demo2-db:db \
        debezium/connect

curl \
	-i \
	-X POST \
	-H "Accept:application/json" \
	-H "Content-Type:application/json" \
	localhost:5300/connectors/ \
	-d '@connector.json'
