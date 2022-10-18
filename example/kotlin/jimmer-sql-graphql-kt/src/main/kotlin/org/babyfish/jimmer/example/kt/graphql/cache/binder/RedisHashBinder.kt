package org.babyfish.jimmer.example.kt.graphql.cache.binder

import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.kt.cache.spi.AbstractKRemoteHashBinder
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.SessionCallback
import java.time.Duration
import java.util.concurrent.TimeUnit

class RedisHashBinder<K, V>(
    private val operations: RedisOperations<String, ByteArray>,
    objectMapper: ObjectMapper?,
    prop: ImmutableProp,
    duration: Duration
) : AbstractKRemoteHashBinder<K, V>(
    objectMapper,
    null,
    prop,
    duration,
    30
) {
    @Suppress("UNCHECKED_CAST")
    override fun read(keys: Collection<String>, hashKey: String): List<ByteArray?> =
        operations.executePipelined(
            object : SessionCallback<Void?> {
                override fun <XK, XV> execute(pops: RedisOperations<XK, XV>): Void? {
                    val pipelinedOps = pops as RedisOperations<String, ByteArray>
                    val hashOps = pipelinedOps.opsForHash<String, ByteArray>()
                    for (key in keys) {
                        hashOps.get(key, hashKey)
                    }
                    return null
                }
            }
        ) as List<ByteArray?>

    @Suppress("UNCHECKED_CAST")
    override fun write(map: Map<String, ByteArray>, hashKey: String) {
        operations.executePipelined(
            object : SessionCallback<Void?> {
                override fun <XK, XV> execute(pops: RedisOperations<XK, XV>): Void? {
                    val pipelinedOps = pops as RedisOperations<String, ByteArray>
                    val hashOps = pipelinedOps.opsForHash<String, ByteArray>()
                    for ((key, value) in map) {
                        hashOps.put(key, hashKey, value)
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
        LOGGER.info("Delete property data from redis: {}", keys)
        operations.delete(keys)
    }

    override fun reason(): String = "redis"

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RedisHashBinder::class.java)
    }
}