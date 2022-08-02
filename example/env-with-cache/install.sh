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
	mysql
docker cp mysql.conf jimmer-demo-mysql:/etc/
docker restart jimmer-demo-mysql
docker cp maxwell.sql jimmer-demo-mysql:/var/maxwell.sql
echo "WAIT mysql for 5 seconds"
sleep 5
docker exec jimmer-demo-mysql /bin/sh -c \
	'mysql -uroot -p123456 </var/maxwell.sql'

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

docker pull ubuntu/kafka
docker run \
	--restart=always \
	-d \
	--network jimmer-demo-network \
	--network-alias jimmer-demo-kafka \
	--name jimmer-demo-kafka \
	-p 4003:9092 \
        -e ZOOKEEPER_HOST=jimmer-demo-zookeeper \
        ubuntu/kafka 
echo "Wait kafka for 5 seconds"
sleep 5
docker exec jimmer-demo-kafka /bin/bash -c \
	'/opt/kafka/bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 5 --topic maxwell'

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
	--kafka_topic=maxwell
