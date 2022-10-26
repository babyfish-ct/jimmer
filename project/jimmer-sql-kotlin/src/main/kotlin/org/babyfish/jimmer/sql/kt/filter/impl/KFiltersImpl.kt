package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.lang.Ref
import org.babyfish.jimmer.sql.filter.Filters
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.filter.KFilters
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KFiltersImpl(
    private val javaFilters: Filters
) : KFilters {

    override fun <T : Any> getFilter(type: KClass<T>, shardingOnly: Boolean): KFilter<T>? =
        javaFilters.getFilter(type.java, shardingOnly)?.toKtFilter()

    override fun <T : Any> getTargetFilter(prop: KProperty1<*, T?>, shardingOnly: Boolean): KFilter<T>? =
        javaFilters.getTargetFilter(prop.toImmutableProp(), shardingOnly)?.toKtFilter()

    override fun getParameterMapRef(type: KClass<*>): Ref<SortedMap<String, Any>?>? =
        javaFilters.getParameterMapRef(type.java)

    override fun getTargetParameterMapRef(prop: KProperty1<*, *>): Ref<SortedMap<String, Any>?>? =
        javaFilters.getTargetParameterMapRef(prop.toImmutableProp())
}