package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.filter.BuiltInFilters
import org.babyfish.jimmer.sql.kt.filter.KBuiltInFilters
import org.babyfish.jimmer.sql.kt.filter.KFilter
import kotlin.reflect.KClass

internal class KBuiltInFiltersImpl(
    private val javaBuiltInFilters: BuiltInFilters
) : KBuiltInFilters {

    override fun <T : Any> getDeclaredNotDeletedFilter(type: KClass<T>): KFilter<T>? =
        javaBuiltInFilters.getDeclaredNotDeletedFilter(type.java)?.toKtFilter()

    override fun <T : Any> getDeclaredAlreadyDeletedFilter(type: KClass<T>): KFilter<T>? =
        javaBuiltInFilters.getDeclaredAlreadyDeletedFilter(type.java).toKtFilter()
}