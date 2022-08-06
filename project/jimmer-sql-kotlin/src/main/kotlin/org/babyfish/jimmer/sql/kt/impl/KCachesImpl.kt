package org.babyfish.jimmer.sql.kt.impl

import com.fasterxml.jackson.databind.JsonNode
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.cache.Caches
import org.babyfish.jimmer.sql.kt.KCaches
import org.babyfish.jimmer.sql.kt.toImmutableProp
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KCachesImpl(
    private val javaCaches: Caches
): KCaches {

    override fun <K, V : Any> getObjectCache(type: KClass<V>): Cache<K, V>? =
        javaCaches.getObjectCache(type.java)

    override fun <K, V> getObjectCache(type: ImmutableType): Cache<K, V>? =
        javaCaches.getObjectCache(type)

    override fun <K, V> getAssociationCache(prop: KProperty1<*, *>): Cache<K, V>? =
        javaCaches.getAssociationCache(prop.toImmutableProp())

    override fun <K, V> getAssociationCache(prop: ImmutableProp): Cache<K, V>? =
        javaCaches.getAssociationCache(prop)

    override fun isAffectedBy(tableName: String): Boolean =
        javaCaches.isAffectedBy(tableName)

    override fun invalidateByBinLog(tableName: String, oldData: JsonNode?, newData: JsonNode?) {
        javaCaches.invalidateByBinLog(tableName, oldData, newData)
    }
}