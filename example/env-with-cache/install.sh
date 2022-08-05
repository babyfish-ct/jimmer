docker network create -d bridge jimmer-demo-network

docker pull mysql
docker run \
	--restart=always \
	-d \
	--name jimmer-demo-mysql \
	--network jimmer-demo-network \
	--network-alias jimmer-demo-mysql \
	-p 4001:3306 \
	-e MYSQL_ROOT_PASSWORD=123456 \
	mysql \
	--lower_case_table_names=1
docker cp mysql.conf jimmer-demo-mysql:/etc/
docker restart jimmer-demo-mysql
echo "WAIT mysql for 5 seconds"
sleep 5
docker cp maxwell.sql jimmer-demo-mysql:/var/maxwell.sql
docker exec jimmer-demo-mysql /bin/sh -c \
	'mysql -uroot -p123456 </var/maxwell.sql'
docker cp jimmer-demo.sql jimmer-demo-mysql:/var/jimmer-demo.sql
docker exec jimmer-demo-mysql /bin/sh -c \
        'mysql -uroot -p123456 </var/jimmer-demo.sql'

docker pull zookeeper
docker run \
	--restart=always \
	-d \
	--network jimmer-demo-network \
        --network-alias jimmer-demo-zookeeper \
	--name jimmer-demo-zookeeper \
	-p 4002:2181 \
	-v /etc/localtime:/etc/localtime \
	zookeeper

docker pull wurstmeister/kafka
docker run \
	--restart=always \
	-d \
	--network jimmer-demo-network \
	--network-alias jimmer-demo-kafka \
	--name jimmer-demo-kafka \
	-p 4003:4003 \
        -e KAFKA_ZOOKEEPER_CONNECT=jimmer-demo-zookeeper:2181 \
	-e KAFKA_ADVERTISED_LISTENERS=INSIDE://jimmer-demo-kafka:9092,OUTSIDE://localhost:4003 \
	-e KAFKA_LISTENERS=INSIDE://:9092,OUTSIDE://:4003 \
	-e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=INSIDE:PLAINTEXT,OUTSIDE:PLAINTEXT \
	-e KAFKA_INTER_BROKER_LISTENER_NAME=INSIDE \
	-e KAFKA_CREATE_TOPICS=maxwell:5:1 \
        wurstmeister/kafka

docker pull zendesk/maxwell
docker run \
	--restart=always \
	-d \
	-it \
        --network jimmer-demo-network \
        --name jimmer-demo-maxwell \
	zendesk/maxwell \
	bin/maxwell \
	--user=maxwell \
	--password=123456 \
	--host=jimmer-demo-mysql \
	--producer=kafka \
	--kafka.bootstrap.servers=jimmer-demo-kafka:9092 \
	--kafka_topic=maxwell \
	--producer_partition_by=primary_key

docker pull redis
docker run \
	--restart=always \
	-d \
	-it \
	--name jimmer-demo-redis \
	--network jimmer-demo-network \
	--network-alias jimmer-demo-redis \
	-p 4004:6379 \
	redis
