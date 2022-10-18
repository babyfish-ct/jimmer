package org.babyfish.jimmer.example.kt.graphql.cache.binder

import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import org.babyfish.jimmer.lang.Ref
import org.babyfish.jimmer.sql.cache.chain.CacheChain
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder
import java.time.Duration

// Level-1 Cache
class CaffeineBinder<K: Any, V: Any>(
    private val maximumSize: Int,
    private val duration: Duration
) : LoadingBinder<K, V> {

    // Caffeine does not support null value, use `Ref<V>` as a wrapper
    private lateinit var loadingCache: LoadingCache<K, Ref<V>>

    override fun initialize(chain: CacheChain<K, V>) {
        loadingCache = Caffeine
            .newBuilder()
            .maximumSize(maximumSize.toLong())
            .expireAfterWrite(duration)
            .build(
                object : CacheLoader<K, Ref<V>> {
                    override fun load(key: K): Ref<V>? {
                        val map = chain.loadAll(setOf(key))
                        val value = map[key]
                        return if (value != null || map.containsKey(key)) {
                            Ref.of(value)
                        } else {
                            null
                        }
                    }

                    override fun loadAll(keys: Iterable<K>): Map<K, Ref<V>> =
                        chain.loadAll((keys as Collection<K>)).mapValues {
                            Ref.of(it.value)
                        }
                }
            )
    }

    override fun getAll(keys: Collection<K>): Map<K, V?> =
        loadingCache.getAll(keys).mapValues {
            it.value.value
        }

    override fun deleteAll(keys: Collection<K>, reason: Any?) {
        if (reason === null || reason == "caffeine") {
            loadingCache.invalidateAll(keys)
        }
    }
}