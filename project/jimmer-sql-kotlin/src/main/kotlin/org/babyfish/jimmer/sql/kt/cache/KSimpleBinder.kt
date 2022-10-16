package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.sql.cache.chain.SimpleBinder
import java.util.*

interface KSimpleBinder<K, V> : SimpleBinder<K, V> {
    
    override fun getAll(keys: Collection<K>): Map<K, V>

    override fun setAll(map: Map<K, V>)

    override fun deleteAll(keys: Collection<K>, reason: Any?)
    
    interface Parameterized<K, V> : KSimpleBinder<K, V>, SimpleBinder.Parameterized<K, V> {

        override fun getAll(keys: Collection<K>): Map<K, V> =
            getAll(keys, Collections.emptySortedMap())

        override fun setAll(map: Map<K, V>) {
            setAll(map, Collections.emptySortedMap())
        }

        override fun getAll(keys: Collection<K>, parameterMap: SortedMap<String, Any>): Map<K, V>

        override fun setAll(map: Map<K, V>, parameterMap: SortedMap<String, Any>)

        override fun deleteAll(keys: Collection<K>, reason: Any?)
    }
}