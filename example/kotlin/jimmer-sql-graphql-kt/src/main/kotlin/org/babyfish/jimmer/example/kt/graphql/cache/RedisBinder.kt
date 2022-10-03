package org.babyfish.jimmer.example.kt.graphql.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.ValueSerializer
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.SessionCallback
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

// Level-2 Cache
class RedisBinder<K, V> private constructor(
    operations: RedisOperations<String, ByteArray>,
    type: ImmutableType?,
    prop: ImmutableProp?,
    duration: Duration,
    randomPercent: Int
) : SimpleBinder<K, V> {

    private val operations: RedisOperations<String, ByteArray>

    private val keyPrefix: String

    private val duration: Duration

    private val randomPercent: Int

    private var valueSerializer: ValueSerializer<V>

    init {
        require(type == null != (prop == null)) { "Illegal metadata" }
        require(!(randomPercent < 0 || randomPercent > 100)) { "randomPercent must between 0 and 100" }
        this.operations = operations
        keyPrefix = if (type != null) {
            "${type.javaClass.simpleName}-"
        } else {
            "${prop!!.declaringType.javaClass.simpleName}.${prop.name}-"
        }
        this.duration = duration
        this.randomPercent = randomPercent
        val mapper = ObjectMapper().registerModule(JavaTimeModule())
        valueSerializer = type
            ?.let { ValueSerializer(it, mapper) }
            ?: ValueSerializer(prop!!, mapper)
    }

    constructor(
        operations: RedisOperations<String, ByteArray>,
        type: ImmutableType,
        duration: Duration,
        randomPercent: Int = 30
    ) : this(operations, type, null, duration, randomPercent)

    constructor(
        operations: RedisOperations<String, ByteArray>,
        prop: ImmutableProp,
        duration: Duration,
        randomPercent: Int = 30
    ) : this(operations, null, prop, duration, randomPercent)

    override fun getAll(keys: Collection<K>): Map<K, V> {
        val values = operations.opsForValue().multiGet(
            keys.map { "$keyPrefix$it" }
        )
        return valueSerializer.deserialize(keys, values!!)
    }

    @Suppress("UNCHECKED_CAST")
    override fun setAll(map: Map<K, V>) {
        val convertedMap = valueSerializer.serialize(map) { "$keyPrefix$it" }
        if (LOGGER.isInfoEnabled) {
            LOGGER.info("save into redis: {}", convertedMap.keys)
        }
        val millis = duration.toMillis()
        val min = millis - randomPercent * millis / 100
        val max = millis + randomPercent * millis / 100
        val random = ThreadLocalRandom.current()
        operations.executePipelined(
            object : SessionCallback<Void?> {
                override fun <XK, XV> execute(pops: RedisOperations<XK, XV>): Void? {
                    val pipelinedOps = pops as RedisOperations<String, ByteArray>
                    pipelinedOps.opsForValue().multiSet(convertedMap)
                    for (key in convertedMap.keys) {
                        pipelinedOps.expire(
                            key,
                            random.nextLong(min, max),
                            TimeUnit.MILLISECONDS
                        )
                    }
                    return null
                }
            }
        )
    }

    override fun deleteAll(keys: Collection<K>, reason: Any?) {
        if (reason === null || reason == "redis") {
            val redisKeys: Collection<String> = keys.map { "$keyPrefix$it" }
            LOGGER.info("delete from redis: {}", redisKeys)
            operations.delete(redisKeys)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RedisBinder::class.java)
    }
}