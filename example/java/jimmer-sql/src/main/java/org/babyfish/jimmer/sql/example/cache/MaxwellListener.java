package org.babyfish.jimmer.sql.example.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.Caches;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@ConditionalOnProperty("spring.redis.host")
@Component
public class MaxwellListener {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final Caches caches;

    public MaxwellListener(JSqlClient sqlClient) {
        this.caches = sqlClient.getCaches();
    }

    @KafkaListener(topics = "maxwell")
    public void onHandle(
            String json,
            Acknowledgment acknowledgment
    ) throws JsonProcessingException {
        JsonNode node = MAPPER.readTree(json);
        String tableName = node.get("table").asText();
        if (caches.isAffectedBy(tableName)) {
            String type = node.get("type").asText();
            JsonNode data = node.get("data");
            switch (type) {
                case "insert":
                    caches.invalidateByBinLog(tableName, null, data);
                    break;
                case "update":
                    caches.invalidateByBinLog(tableName, node.get("old"), data);
                    break;
                case "delete":
                    caches.invalidateByBinLog(tableName, data, null);
                    break;
            }
        }
        acknowledgment.acknowledge();
    }
}
