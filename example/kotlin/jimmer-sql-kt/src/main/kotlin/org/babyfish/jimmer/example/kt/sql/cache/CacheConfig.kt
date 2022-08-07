package org.babyfish.jimmer.example.kt.sql.cache

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
            valueSerializer =
                object : RedisSerializer<ByteArray?> {
                    override fun serialize(t: ByteArray?): ByteArray? = t
                    override fun deserialize(bytes: ByteArray?): ByteArray? = bytes
                }
        }

    @Bean
    fun cacheFactory(redisTemplate: RedisTemplate<String, ByteArray>): CacheFactory =
        object : CacheFactory {
            override fun createObjectCache(type: ImmutableType): Cache<*, *> {
                return ChainCacheBuilder<Any, Any>()
                    .add(CaffeineBinder(512, Duration.ofSeconds(1)))
                    .add(
                        RedisBinder(redisTemplate, type, Duration.ofHours(10))
                    )
                    .build()
            }

            override fun createAssociatedIdCache(prop: ImmutableProp): Cache<*, *> {
                return ChainCacheBuilder<Any, Any>()
                    .add(CaffeineBinder(512, Duration.ofSeconds(1)))
                    .add(
                        RedisBinder(redisTemplate, prop, Duration.ofHours(5))
                    )
                    .build()
            }

            override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>> {
                return ChainCacheBuilder<Any, List<*>>()
                    .add(CaffeineBinder(64, Duration.ofSeconds(1)))
                    .add(
                        RedisBinder(redisTemplate, prop, Duration.ofHours(5))
                    )
                    .build()
            }
        }
}