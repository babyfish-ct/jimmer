package org.babyfish.jimmer.example.kt.graphql.cfg

import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.sql.event.binlog.BinLog
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@ConditionalOnProperty("spring.redis.host")
@Component
class MaxwellListener(sqlClient: KSqlClient) {

    private val binLog: BinLog = sqlClient.binLog

    @KafkaListener(topics = ["maxwell"])
    fun onHandle(
        json: String,
        acknowledgment: Acknowledgment
    ) {
        val node = MAPPER.readTree(json)
        val tableName = node["table"].asText()
        val type = node["type"].asText()
        val data = node["data"]
        when (type) {
            "insert" ->
                binLog.accept(tableName, null, data)
            "update" ->
                binLog.accept(tableName, node["old"], data)
            "delete" ->
                binLog.accept(tableName, data, null)
        }
        acknowledgment.acknowledge()
    }

    companion object {
        private val MAPPER = ObjectMapper()
    }
}