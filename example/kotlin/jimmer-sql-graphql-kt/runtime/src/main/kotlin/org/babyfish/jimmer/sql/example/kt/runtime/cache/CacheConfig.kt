package org.babyfish.jimmer.sql.example.kt.runtime.cache

import com.fasterxml.jackson.databind.ObjectMapper
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.spring.cache.CaffeineBinder
import org.babyfish.jimmer.spring.cache.RedisCaches
import org.babyfish.jimmer.spring.cache.RedisHashBinder
import org.babyfish.jimmer.spring.cache.RedisValueBinder
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.cache.CacheFactory
import org.babyfish.jimmer.sql.cache.chain.*
import org.babyfish.jimmer.sql.example.kt.model.BookStore
import org.babyfish.jimmer.sql.kt.cache.AbstractKCacheFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration

// -----------------------------
// If you are a beginner, please ignore this class,
// for non-cache mode, this class will never be used.
// -----------------------------
@ConditionalOnProperty("spring.redis.host")
@Configuration
class CacheConfig {

    @Bean
    fun cacheFactory( // ❶
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper
    ): CacheFactory {

        val redisTemplate = RedisCaches.cacheRedisTemplate(connectionFactory)

        /*
         * Single-view caches:
         *      - All object caches
         *      - `Book.store`
         *      - `Book.authors`
         *      - `TreeNode.parent`
         *      - `TreeNode.childNodes`
         *
         * Multiple-view caches:
         *      - `BookStore.books`
         *      - `Author.books`
         *      - `BookStore.avgPrice`
         *      - `BookStore.newestBooks`
         */
        return object : AbstractKCacheFactory() {

            // Id -> Object
            override fun createObjectCache(type: ImmutableType): Cache<*, *>? = // ❷
                ChainCacheBuilder<Any, Any>()
                    .add(CaffeineBinder(512, Duration.ofSeconds(1)))
                    .add(RedisValueBinder(redisTemplate, objectMapper, type, Duration.ofMinutes(10)))
                    .build()

            // Id -> TargetId, for one-to-one/many-to-one
            override fun createAssociatedIdCache(prop: ImmutableProp): Cache<*, *>? = // ❸
                createPropCache<Any, Any>(
                    filterState.isAffected(prop.targetType), // ❹
                    prop,
                    redisTemplate,
                    objectMapper,
                    Duration.ofMinutes(5)
                )

            // Id -> TargetId list, for one-to-many/many-to-many
            override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>>? = // ❺
                createPropCache<Any, List<*>>(
                    filterState.isAffected(prop.targetType), // ❻
                    prop,
                    redisTemplate,
                    objectMapper,
                    Duration.ofMinutes(5)
                )

            // Id -> computed value, for transient properties with resolver
            override fun createResolverCache(prop: ImmutableProp): Cache<*, *>? = // ❼
                createPropCache<Any, Any>(
                    prop == BookStore::avgPrice.toImmutableProp() ||
                        prop == BookStore::newestBooks.toImmutableProp(),
                    prop,
                    redisTemplate,
                    objectMapper,
                    Duration.ofHours(1)
                )
        }
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
            if (isMultiView) { // ❽
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

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/cache/enable-cache
❷ https://babyfish-ct.github.io/jimmer/docs/cache/cache-type/object
❸ ❺ https://babyfish-ct.github.io/jimmer/docs/cache/cache-type/association
❹ ❻ https://babyfish-ct.github.io/jimmer/docs/cache/multiview-cache/user-filter#better-approach
❼ https://babyfish-ct.github.io/jimmer/docs/cache/cache-type/calculation
❽ https://babyfish-ct.github.io/jimmer/docs/cache/multiview-cache/concept
---------------------------------------------------*/
