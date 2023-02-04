package org.babyfish.jimmer.example.kt.sql.cfg

import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.spring.cache.CaffeineBinder
import org.babyfish.jimmer.spring.cache.RedisHashBinder
import org.babyfish.jimmer.spring.cache.RedisValueBinder
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.cache.CacheFactory
import org.babyfish.jimmer.sql.cache.chain.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@ConditionalOnProperty("spring.redis.host")
@Configuration
class CacheConfig {

    @Bean
    fun rawDataRedisTemplate(
        connectionFactory: RedisConnectionFactory
    ): RedisTemplate<String, ByteArray> =
        RedisTemplate<String, ByteArray>().apply {

            setConnectionFactory(connectionFactory)

            val nopSerializer = object : RedisSerializer<ByteArray?> {
                override fun serialize(t: ByteArray?): ByteArray? = t
                override fun deserialize(bytes: ByteArray?): ByteArray? = bytes
            }
            keySerializer = StringRedisSerializer.UTF_8
            valueSerializer = nopSerializer
            hashKeySerializer = StringRedisSerializer.UTF_8
            hashValueSerializer = nopSerializer
        }

    @Bean
    fun cacheFactory(
        redisTemplate: RedisTemplate<String, ByteArray>,
        objectMapper: ObjectMapper
    ): CacheFactory =
        object : CacheFactory {

            // Id -> Object
            override fun createObjectCache(type: ImmutableType): Cache<*, *>? =
                ChainCacheBuilder<Any, Any>()
                    .add(CaffeineBinder(512, Duration.ofSeconds(1)))
                    .add(RedisValueBinder(redisTemplate, objectMapper, type, Duration.ofMinutes(10)))
                    .build()

            // Id -> TargetId, for one-to-one/many-to-one
            override fun createAssociatedIdCache(prop: ImmutableProp): Cache<*, *>? =
                ChainCacheBuilder<Any, Any>()
                    .add(RedisHashBinder(redisTemplate, objectMapper, prop, Duration.ofMinutes(5)))
                    .build()

            // Id -> TargetId list, for one-to-many/many-to-many
            override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>>? =
                ChainCacheBuilder<Any, List<*>>()
                    .add(RedisHashBinder(redisTemplate, objectMapper, prop, Duration.ofMinutes(5)))
                    .build()

            // Id -> computed value, for transient properties with resolver
            override fun createResolverCache(prop: ImmutableProp): Cache<*, *>? =
                ChainCacheBuilder<Any, List<*>>()
                    .add(RedisHashBinder(redisTemplate, objectMapper, prop, Duration.ofHours(1)))
                    .build()
        }
}