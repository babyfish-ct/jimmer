package org.babyfish.jimmer.example.kt.graphql.cache

import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import org.babyfish.jimmer.sql.cache.chain.CacheChain
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder
import java.time.Duration
import java.util.*

// Level-1 Cache
class CaffeineBinder<K: Any, V: Any>(
    private val maximumSize: Int, 
    private val duration: Duration
) : LoadingBinder<K, V> {
    
    // Caffeine does not support null value, use optional as a wrapper
    private lateinit var loadingCache: LoadingCache<K, Optional<V>>
    
    override fun initialize(chain: CacheChain<K, V>) {
        loadingCache = Caffeine
            .newBuilder()
            .maximumSize(maximumSize.toLong())
            .expireAfterWrite(duration)
            .build(
                object : CacheLoader<K, Optional<V>> {
                    override fun load(key: K): Optional<V> {
                        val map = chain.loadAll(setOf(key))
                        val value = map[key]
                        return if (value != null || map.containsKey(key)) {
                            Optional.ofNullable(value)
                        } else {
                            Optional.ofNullable(null)
                        }
                    }

                    override fun loadAll(keys: Set<K>): Map<out K, Optional<V>> =
                        chain.loadAll((keys as Collection<K>)).mapValues {
                            Optional.ofNullable(it.value)
                        }
                }
            )
    }

    override fun getAll(keys: Collection<K>): Map<K, V> =
        loadingCache.getAll(keys).mapValues {
            it.value.orElse(null)
        }

    override fun deleteAll(keys: Collection<K>, reason: Any?) {
        if (reason === null || reason == "caffeine") {
            loadingCache.invalidateAll(keys)
        }
    }
}