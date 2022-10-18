package org.babyfish.jimmer.sql.kt.common

import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.cache.chain.CacheChain
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
import org.babyfish.jimmer.sql.kt.cache.KLoadingBinder
import org.babyfish.jimmer.sql.kt.cache.KSimpleBinder

fun <K, V> createCache(): Cache<K, V> =
    ChainCacheBuilder<K, V>()
        .add(LevelOneBinder())
        .add(LevelTwoBinder())
        .build()

private class LevelOneBinder<K, V> : KLoadingBinder<K, V> {

    private lateinit var chain: CacheChain<K, V>

    private val valueMap = mutableMapOf<K, V>()

    override fun initialize(chain: CacheChain<K, V>) {
        this.chain = chain
    }

    @Suppress("UNCHECKED_CAST")
    override fun getAll(keys: Collection<K>): Map<K, V> {
        val map = mutableMapOf<K, V>()
        val missedKeys = mutableSetOf<K>()
        for (key in keys) {
            val value = valueMap[key]
            if (value !== null) {
                map[key] = value
            } else if (valueMap.containsKey(key)) {
                map[key] = null as V
            } else {
                missedKeys += key
            }
        }
        if (missedKeys.isNotEmpty()) {
            val moreMap = chain.loadAll(missedKeys)
            map += moreMap
            valueMap += moreMap
        }
        return map
    }

    override fun deleteAll(keys: Collection<K>, reason: Any?) {
        valueMap.keys.removeAll(keys.toSet())
    }
}

private class LevelTwoBinder<K, V> : KSimpleBinder<K, V> {

    private val valueMap = mutableMapOf<K, V>()

    override fun getAll(keys: Collection<K>): Map<K, V> {
        val map = mutableMapOf<K, V>()
        for (key in keys) {
            val value = valueMap[key]
            if (value !== null) {
                map[key] = value
            } else if (valueMap.containsKey(key)) {
                map[key] = null as V
            }
        }
        return map
    }

    override fun setAll(map: Map<K, V>) {
        valueMap += map
    }

    override fun deleteAll(keys: Collection<K>, reason: Any?) {
        valueMap.keys.removeAll(keys.toSet())
    }
}