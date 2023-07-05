package org.babyfish.jimmer.sql.example.cfg.cache

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.sql.event.binlog.BinLog
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment


// -----------------------------
// If you are a beginner, please ignore this class,
// for non-cache mode, this class will never be used.
// -----------------------------
@ConditionalOnProperty(
    name = ["spring.profiles.active"],
    havingValue = "debezium"
)
class DebeziumListener(sqlClient: KSqlClient) {

    private val binLog: BinLog = sqlClient.binLog

    @KafkaListener(topicPattern = """debezium\..*""")
    @Throws(JsonProcessingException::class)
    fun onDebeziumEvent(
        json: String,
        acknowledgment: Acknowledgment
    ) {
        val node: JsonNode = MAPPER.readTree(json)
        val tableName: String = node.get("source").get("table").asText()
        binLog.accept(
            tableName,
            node.get("before"),
            node.get("after")
        )
        acknowledgment.acknowledge()
    }

    companion object {
        private val MAPPER = ObjectMapper()
    }
}