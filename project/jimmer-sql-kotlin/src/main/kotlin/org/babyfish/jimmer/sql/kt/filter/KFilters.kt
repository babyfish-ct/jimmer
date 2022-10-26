package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.lang.Ref
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KFilters {

    fun <T: Any> getFilter(type: KClass<T>, shardingOnly: Boolean = false): KFilter<T>?

    fun <T: Any> getTargetFilter(prop: KProperty1<*, T?>, shardingOnly: Boolean = false): KFilter<T>?

    fun getParameterMapRef(type: KClass<*>): Ref<SortedMap<String, Any>?>?

    fun getTargetParameterMapRef(prop: KProperty1<*, *>): Ref<SortedMap<String, Any>?>?
}