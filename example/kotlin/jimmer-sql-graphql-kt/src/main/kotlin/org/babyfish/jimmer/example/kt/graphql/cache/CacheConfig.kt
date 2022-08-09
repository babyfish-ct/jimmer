package org.babyfish.jimmer.example.kt.graphql.cache

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
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
            keySerializer = StringRedisSerializer.UTF_8

            // The nullability in the business sense is different from the nullability of redis values.
            // Specify a dummy serializer for spring redis and use
            // `org.babyfish.jimmer.sql.example.cache.ValueSerializer` to handle the values.
            valueSerializer =
                object : RedisSerializer<ByteArray?> {
                    override fun serialize(t: ByteArray?): ByteArray? = t
                    override fun deserialize(bytes: ByteArray?): ByteArray? = bytes
                }
        }

    @Bean
    fun cacheFactory(redisTemplate: RedisTemplate<String, ByteArray>): CacheFactory =
        object : CacheFactory {

            // Id -> Object
            override fun createObjectCache(type: ImmutableType): Cache<*, *>? =
                ChainCacheBuilder<Any, Any>()
                    .add(CaffeineBinder(512, Duration.ofSeconds(1)))
                    .add(RedisBinder(redisTemplate, type, Duration.ofMinutes(10)))
                    .build()

            // Id -> TargetId, for one-to-one/one-to-many
            override fun createAssociatedIdCache(prop: ImmutableProp): Cache<*, *>? =
                ChainCacheBuilder<Any, Any>()
                    .add(CaffeineBinder(512, Duration.ofSeconds(1)))
                    .add(RedisBinder(redisTemplate, prop, Duration.ofMinutes(5)))
                    .build()

            // Id -> TargetId List, for one-to-many/many-to-many
            override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>>? =
                ChainCacheBuilder<Any, List<*>>()
                    .add(CaffeineBinder(64, Duration.ofSeconds(1)))
                    .add(RedisBinder(redisTemplate, prop, Duration.ofMinutes(5)))
                    .build()

            // Id -> computed value, for transient properties with resolver
            override fun createResolverCache(prop: ImmutableProp): Cache<*, *>? =
                ChainCacheBuilder<Any, List<*>>()
                    .add(CaffeineBinder(1024, Duration.ofSeconds(1)))
                    .add(RedisBinder(redisTemplate, prop, Duration.ofHours(1)))
                    .build()
        }
}