package org.babyfish.jimmer.example.kt.graphql.cfg

import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.example.kt.graphql.entities.BookStore
import org.babyfish.jimmer.example.kt.graphql.entities.common.TenantAware
import org.babyfish.jimmer.kt.toImmutableProp
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

// -----------------------------
// If you are a beginner, please ignore this class,
// for non-cache mode, this class will never be used.
// -----------------------------
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
                createPropCache<Any, Any>(
                    TenantAware::class.java.isAssignableFrom(prop.targetType.javaClass),
                    prop,
                    redisTemplate,
                    objectMapper,
                    Duration.ofMinutes(5)
                )

            // Id -> TargetId list, for one-to-many/many-to-many
            override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>>? =
                createPropCache<Any, List<*>>(
                    TenantAware::class.java.isAssignableFrom(prop.targetType.javaClass),
                    prop,
                    redisTemplate,
                    objectMapper,
                    Duration.ofMinutes(5)
                )

            // Id -> computed value, for transient properties with resolver
            override fun createResolverCache(prop: ImmutableProp): Cache<*, *>? =
                createPropCache<Any, Any>(
                    prop == BookStore::avgPrice.toImmutableProp() ||
                        prop == BookStore::newestBooks.toImmutableProp(),
                    prop,
                    redisTemplate,
                    objectMapper,
                    Duration.ofHours(1)
                )
        }

    companion object {

        @JvmStatic
        private fun <K, V> createPropCache(
            isMultiView: Boolean,
            prop: ImmutableProp,
            redisTemplate: RedisTemplate<String, ByteArray>,
            objectMapper: ObjectMapper,
            redisDuration: Duration
        ): Cache<K, V> {
            /*
             * If multi-view cache is required, only redis can be used, because redis support hash structure.
             * The value of redis hash is a nested map, so that different users can see different data.
             *
             * Other simple key value caches can be divided into two levels.
             * The first level is caffeine, the second level is redis.
             *
             * Note: Once the multi-view cache takes affect, it will consume
             * a lot of cache space, please only use it for important data.
             */
            if (isMultiView) {
                return ChainCacheBuilder<K, V>()
                    .add(RedisHashBinder(redisTemplate, objectMapper, prop, redisDuration))
                    .build()
            }

            return ChainCacheBuilder<K, V>()
                .add(CaffeineBinder(512, Duration.ofSeconds(1)))
                .add(RedisValueBinder(redisTemplate, objectMapper, prop, redisDuration))
                .build()
        }
    }
}