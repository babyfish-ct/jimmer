package org.babyfish.jimmer.sql.example.cfg.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

// -----------------------------
// If you are a beginner, please ignore this class,
// for non-cache mode, this class will never be used.
// -----------------------------
@ConditionalOnProperty(
        name = "spring.profiles.active",
        havingValue = "debezium"
)
@Component
public class DebeziumListener {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final BinLog binLog;

    public DebeziumListener(JSqlClient sqlClient) {
        this.binLog = sqlClient.getBinLog();
    }

    @KafkaListener(topicPattern = "debezium\\..*")
    public void onDebeziumEvent(
            String json,
            Acknowledgment acknowledgment
    ) throws JsonProcessingException {
        JsonNode node = MAPPER.readTree(json);
        String tableName = node.get("source").get("table").asText();
        binLog.accept(
                tableName,
                node.get("before"),
                node.get("after")
        );
        acknowledgment.acknowledge();
    }
}
