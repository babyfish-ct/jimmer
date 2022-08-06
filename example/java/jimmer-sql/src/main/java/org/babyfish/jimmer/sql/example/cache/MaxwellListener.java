package org.babyfish.jimmer.sql.example.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@ConditionalOnProperty("spring.redis.host")
@Component
public class MaxwellListener {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @KafkaListener(topics = "maxwell")
    public void onHandle(
            String json,
            Acknowledgment acknowledgment
    ) throws JsonProcessingException {
        JsonNode node = MAPPER.readTree(json);
        System.out.println(node);
        acknowledgment.acknowledge();
    }
}
