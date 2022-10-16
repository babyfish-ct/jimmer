package org.babyfish.jimmer.sql.kt.common

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.cache.chain.CacheChain
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
import org.babyfish.jimmer.sql.kt.cache.KLoadingBinder
import org.babyfish.jimmer.sql.kt.cache.KSimpleBinder
import java.util.*

fun <K, V> createParameterizedCache(
    prop: ImmutableProp? = null,
    onDelete: ((ImmutableProp, Collection<K>) -> Unit)? = null
): Cache.Parameterized<K, V> =
    ChainCacheBuilder<K, V>()
        .add(LevelOneParameterizedBinder())
        .add(LevelTwoParameterizedBinder(prop, onDelete))
        .build() as Cache.Parameterized<K, V>

@Suppress("UNCHECKED_CAST")
private fun <K, V> read(
    valueMap: MutableMap<K, MutableMap<SortedMap<String, Any>, V>>,
    keys: Collection<K>,
    parameterMap: SortedMap<String, Any>
): MutableMap<K, V> {
    val map = mutableMapOf<K, V>()
    for (key in keys) {
        val subMap = valueMap[key]
        if (subMap != null) {
            val value = subMap[parameterMap]
            if (value != null || subMap.containsKey(parameterMap)) {
                map[key] = value as V
            }
        }
    }
    return map
}

private fun <K, V> write(
    valueMap: MutableMap<K, MutableMap<SortedMap<String, Any>, V>>,
    map: Map<K, V>,
    parameterMap: SortedMap<String, Any>
) {
    for ((key, value) in map) {
        val subMap = valueMap.computeIfAbsent(key) { HashMap() }
        subMap[parameterMap] = value
    }
}

private class LevelOneParameterizedBinder<K, V> : KLoadingBinder.Parameterized<K, V> {

    private lateinit var chain: CacheChain.Parameterized<K, V>

    private val valueMap = mutableMapOf<K, MutableMap<SortedMap<String, Any>, V>>()

    override fun initialize(chain: CacheChain.Parameterized<K, V>) {
        this.chain = chain
    }

    override fun getAll(keys: Collection<K>, parameterMap: SortedMap<String, Any>): MutableMap<K, V> {
        val map = read(valueMap, keys, parameterMap)
        if (map.size < keys.size) {
            val missedKeys: MutableSet<K> = LinkedHashSet()
            for (key in keys) {
                if (!map.containsKey(key)) {
                    missedKeys.add(key)
                }
            }
            val mapFromNext = chain.loadAll(keys, parameterMap)
            if (mapFromNext.size < missedKeys.size) {
                for (missedKey in missedKeys) {
                    if (!mapFromNext.containsKey(missedKey)) {
                        mapFromNext[missedKey] = null
                    }
                }
            }
            write(valueMap, mapFromNext, parameterMap)
            map += mapFromNext
        }
        return map
    }

    override fun deleteAll(keys: Collection<K>, reason: Any?) {
        valueMap.keys.removeAll(keys.toSet())
    }
}

private class LevelTwoParameterizedBinder<K, V>(
    private val prop: ImmutableProp?,
    private val onDelete: ((ImmutableProp, Collection<K>) -> Unit)?
) : KSimpleBinder.Parameterized<K, V> {

    private val valueMap = mutableMapOf<K, MutableMap<SortedMap<String, Any>, V>>()

    override fun getAll(
        keys: Collection<K>,
        parameterMap: SortedMap<String, Any>
    ): Map<K, V> =
        read(valueMap, keys, parameterMap)

    override fun setAll(map: Map<K, V>, parameterMap: SortedMap<String, Any>) {
        write(valueMap, map, parameterMap)
    }

    override fun deleteAll(keys: Collection<K>, reason: Any?) {
        valueMap.keys.removeAll(keys.toSet())
        if (prop !== null) {
            onDelete?.invoke(prop, keys)
        }
    }
}