package org.babyfish.jimmer.sql.kt

import com.fasterxml.jackson.databind.JsonNode
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.Cache
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KCaches {

    fun <K, V: Any> getObjectCache(type: KClass<V>): Cache<K, V>?

    fun <K, V> getObjectCache(type: ImmutableType): Cache<K, V>?

    fun <K, V> getAssociationCache(prop: KProperty1<*, *>): Cache<K, V>?

    fun <K, V> getAssociationCache(prop: ImmutableProp): Cache<K, V>?

    fun isAffectedBy(tableName: String): Boolean

    fun invalidateByBinLog(tableName: String, oldData: JsonNode?, newData: JsonNode?)
}