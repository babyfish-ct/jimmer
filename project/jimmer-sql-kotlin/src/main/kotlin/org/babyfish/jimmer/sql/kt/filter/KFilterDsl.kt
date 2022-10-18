package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.filter.FilterConfig
import org.babyfish.jimmer.sql.kt.filter.impl.toJavaFilter
import kotlin.reflect.KClass

class KFilterDsl internal constructor(
    private val javaConfig: FilterConfig
) {
    fun enable(vararg filters: KFilter<*>) {
        javaConfig.enable(filters.map { it.toJavaFilter() })
    }

    fun enable(filters: Collection<KFilter<*>>) {
        javaConfig.enable(filters.map { it.toJavaFilter() })
    }

    fun disable(vararg filters: KFilter<*>) {
        javaConfig.disable(filters.map { it.toJavaFilter() })
    }

    fun disable(filters: Collection<KFilter<*>>) {
        javaConfig.disable(filters.map { it.toJavaFilter() })
    }

    fun enableByTypes(vararg filterTypes: KClass<*>) {
        javaConfig.enableByTypes(filterTypes.map { it.java })
    }

    fun enableByTypes(filterTypes: Collection<KClass<*>>) {
        javaConfig.enableByTypes(filterTypes.map { it.java })
    }

    fun disableByTypes(vararg filterTypes: KClass<*>) {
        javaConfig.disableByTypes(filterTypes.map { it.java })
    }

    fun disableByTypes(filterTypes: Collection<KClass<*>>) {
        javaConfig.disableByTypes(filterTypes.map { it.java })
    }
}