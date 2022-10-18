package org.babyfish.jimmer.example.kt.graphql.cache.binder

import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.kt.cache.spi.AbstractKRemoteValueBinder
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.SessionCallback
import java.time.Duration
import java.util.concurrent.TimeUnit

class RedisValueBinder<K, V>(
    private val operations: RedisOperations<String, ByteArray>,
    objectMapper: ObjectMapper?,
    type: ImmutableType,
    duration: Duration
) : AbstractKRemoteValueBinder<K, V>(
    objectMapper,
    type,
    null,
    duration,
    30
) {
    override fun read(keys: Collection<String>): List<ByteArray?> =
        operations.opsForValue().multiGet(keys)!!

    @Suppress("UNCHECKED_CAST")
    override fun write(map: Map<String, ByteArray>) {
        operations.executePipelined(
            object : SessionCallback<Void?> {
                override fun <XK, XV> execute(pops: RedisOperations<XK, XV>): Void? {
                    val pipelinedOps = pops as RedisOperations<String, ByteArray>
                    pipelinedOps.opsForValue().multiSet(map)
                    for (key in map.keys) {
                        pipelinedOps.expire(
                            key,
                            nextExpireMillis(),
                            TimeUnit.MILLISECONDS
                        )
                    }
                    return null
                }
            }
        )
    }

    override fun delete(keys: Collection<String>) {
        LOGGER.info("Delete object data from redis: {}", keys)
        operations.delete(keys)
    }

    override fun reason(): String = "redis"

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RedisValueBinder::class.java)
    }
}