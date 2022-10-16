package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.sql.filter.Filters
import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.filter.KFilters
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KFiltersImpl(
    private val javaFilters: Filters
) : KFilters {

    override fun <T : Any> getFilter(type: KClass<T>): KFilter<T>? =
        javaFilters.getFilter(type.java)?.let {
            it.toKtFilter()
        }

    override fun <T : Any> getTargetFilter(prop: KProperty1<Any, T?>): KFilter<T>? =
        javaFilters.getTargetFilter(prop.toImmutableProp())?.let {
            it.toKtFilter()
        }

    override fun <T : Any> getCacheableFilter(type: KClass<T>): KCacheableFilter<T>? =
        getFilter(type) as? KCacheableFilter<T>

    override fun <T : Any> getCacheableTargetFilter(prop: KProperty1<Any, T?>): KCacheableFilter<T>? =
        getTargetFilter(prop) as? KCacheableFilter<T>
}