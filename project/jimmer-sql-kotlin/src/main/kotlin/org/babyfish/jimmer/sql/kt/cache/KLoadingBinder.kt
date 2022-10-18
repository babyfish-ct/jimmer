package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.sql.cache.chain.CacheChain
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder
import java.util.*

interface KLoadingBinder<K, V> : LoadingBinder<K, V> {

    override fun initialize(chain: CacheChain<K, V>)

    override fun getAll(keys: Collection<K>): Map<K, V>

    override fun deleteAll(keys: Collection<K>, reason: Any?)

    interface Parameterized<K, V> : LoadingBinder.Parameterized<K, V> {

        override fun initialize(chain: CacheChain.Parameterized<K, V>)

        override fun getAll(keys: Collection<K>): Map<K, V> =
            getAll(keys, Collections.emptySortedMap())

        override fun getAll(
            keys: Collection<K>,
            parameterMap: SortedMap<String, Any>
        ): MutableMap<K, V>

        override fun deleteAll(keys: Collection<K>, reason: Any?)
    }
}