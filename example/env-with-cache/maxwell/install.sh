docker run \
    --restart=always \
    -d \
    --name maxwell-demo-zookeeper \
    -p 4000:2181 \
    -v /etc/localtime:/etc/localtime \
    zookeeper

docker run \
    --restart=always \
    -d \
    --name maxwell-demo-kafka \
    --hostname kafka \
    -p 4100:4100 \
    -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
    -e KAFKA_ADVERTISED_LISTENERS=INSIDE://kafka:9092,OUTSIDE://localhost:4100 \
    -e KAFKA_LISTENERS=INSIDE://:9092,OUTSIDE://:4100 \
    -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=INSIDE:PLAINTEXT,OUTSIDE:PLAINTEXT \
    -e KAFKA_INTER_BROKER_LISTENER_NAME=INSIDE \
    -e KAFKA_CREATE_TOPICS=maxwell:1:1 \
    --link maxwell-demo-zookeeper:zookeeper \
    wurstmeister/kafka

docker run \
    --restart=always \
    -it \
    -d \
    --name maxwell-demo-kafdrop \
    -p 4101:9000 \
    -e KAFKA_BROKERCONNECT=kafka:9092 \
    --link maxwell-demo-kafka:kafka \
    linuxforhealth/kafdrop

docker run \
    --restart=always \
    -d \
    --name maxwell-demo-mysql \
    -p 4200:3306 \
    -e MYSQL_DATABASE=jimmer_demo \
    -e MYSQL_ROOT_PASSWORD=123456 \
    mysql \
    --lower_case_table_names=1
docker cp mysql.conf maxwell-demo-mysql:/etc/
docker restart maxwell-demo-mysql

echo "WAIT mysql for 5 seconds..."
sleep 5

docker cp maxwell.sql maxwell-demo-mysql:/var/maxwell.sql
docker exec maxwell-demo-mysql /bin/sh -c \
    'mysql -uroot -p123456 </var/maxwell.sql'
docker cp jimmer-demo.sql maxwell-demo-mysql:/var/jimmer-demo.sql
docker exec maxwell-demo-mysql /bin/sh -c \
    'mysql -uroot -p123456 </var/jimmer-demo.sql'

docker run \
    --restart=always \
    -d \
    -it \
    --name maxwell-demo-maxwell \
    --link maxwell-demo-mysql:mysql \
    --link maxwell-demo-kafka:kakfa \
    zendesk/maxwell \
        bin/maxwell \
        --host=mysql \
        --user=maxwell \
        --password=123456 \
        --producer=kafka \
        --kafka.bootstrap.servers=kafka:9092 \
        --kafka_topic=maxwell \
        --producer_partition_by=primary_key

docker run \
    --restart=always \
    -d \
    -it \
    --name maxwell-demo-redis \
    -p 4400:6379 \
    redis
