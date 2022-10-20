package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.sql.filter.Filters
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

    override fun <T : Any> getTargetFilter(prop: KProperty1<*, T?>): KFilter<T>? =
        javaFilters.getTargetFilter(prop.toImmutableProp())?.let {
            it.toKtFilter()
        }

    override fun <T : Any> getParameterizedFilter(type: KClass<T>): KFilter.Parameterized<T>? =
        getFilter(type) as? KFilter.Parameterized<T>

    override fun <T : Any> getParameterizedTargetFilter(prop: KProperty1<*, T?>): KFilter.Parameterized<T>? =
        getTargetFilter(prop) as? KFilter.Parameterized<T>
}