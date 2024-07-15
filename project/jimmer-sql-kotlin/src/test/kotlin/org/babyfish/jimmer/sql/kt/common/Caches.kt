package org.babyfish.jimmer.sql.kt.common

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.cache.chain.CacheChain
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
import org.babyfish.jimmer.sql.kt.cache.KLoadingBinder
import org.babyfish.jimmer.sql.kt.cache.KSimpleBinder

fun <K, V> createCache(type: ImmutableType): Cache<K, V> =
    ChainCacheBuilder<K, V>()
        .add(LevelOneBinder(type, null))
        .add(LevelTwoBinder(type, null, null))
        .build()

fun <K, V> createCache(
    prop: ImmutableProp,
    onDelete: ((ImmutableProp, Collection<K>) -> Unit)? = null
): Cache<K, V> =
    ChainCacheBuilder<K, V>()
        .add(LevelOneBinder(prop.declaringType, prop))
        .add(LevelTwoBinder(prop.declaringType, prop, onDelete))
        .build()

private class LevelOneBinder<K, V>(
    private val type: ImmutableType,
    private val prop: ImmutableProp?
) : KLoadingBinder<K, V> {

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

    override fun type(): ImmutableType =
        type

    override fun prop(): ImmutableProp? =
        prop
}

private class LevelTwoBinder<K, V>(
    private val type: ImmutableType,
    private val prop: ImmutableProp?,
    private val onDelete: ((ImmutableProp, Collection<K>) -> Unit)? = null
) : KSimpleBinder<K, V> {

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
        if (prop !== null) {
            onDelete?.invoke(prop, keys)
        }
    }

    override fun type(): ImmutableType = type

    override fun prop(): ImmutableProp? = prop
}