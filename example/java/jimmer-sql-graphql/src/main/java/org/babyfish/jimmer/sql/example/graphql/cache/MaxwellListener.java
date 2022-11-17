package org.babyfish.jimmer.sql.example.graphql.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@ConditionalOnProperty("spring.redis.host")
@Component
public class MaxwellListener {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BinLog binLog;

    public MaxwellListener(JSqlClient sqlClient) {
        this.binLog = sqlClient.getBinLog();
    }

    @KafkaListener(topics = "maxwell")
    public void onHandle(
            String json,
            Acknowledgment acknowledgment
    ) throws JsonProcessingException {
        JsonNode node = MAPPER.readTree(json);
        String tableName = node.get("table").asText();
        String type = node.get("type").asText();
        JsonNode data = node.get("data");
        switch (type) {
            case "insert":
                binLog.accept(tableName, null, data);
                break;
            case "update":
                binLog.accept(tableName, node.get("old"), data);
                break;
            case "delete":
                binLog.accept(tableName, data, null);
                break;
        }
        acknowledgment.acknowledge();
    }
}
