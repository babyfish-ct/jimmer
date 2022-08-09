package org.babyfish.jimmer.example.kt.sql.cache

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.babyfish.jimmer.sql.kt.KCaches
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component


@ConditionalOnProperty("spring.redis.host")
@Component
class MaxwellListener(sqlClient: KSqlClient) {

    private val caches: KCaches = sqlClient.caches

    @KafkaListener(topics = ["maxwell"])
    fun onHandle(
        json: String,
        acknowledgment: Acknowledgment
    ) {
        val node = MAPPER.readTree(json)
        val tableName = node["table"].asText()
        if (caches.isAffectedBy(tableName)) {
            val type = node["type"].asText()
            val data = node["data"]
            when (type) {
                "insert" ->
                    caches.invalidateByBinLog(tableName, null, data)
                "update" -> {
                    val old = node["old"] as ObjectNode
                    old.set<JsonNode>("id", data["id"])
                    caches.invalidateByBinLog(tableName, old, data)
                }
                "delete" ->
                    caches.invalidateByBinLog(tableName, data, null)
            }
        }
        acknowledgment.acknowledge()
    }

    companion object {
        private val MAPPER = ObjectMapper()
    }
}