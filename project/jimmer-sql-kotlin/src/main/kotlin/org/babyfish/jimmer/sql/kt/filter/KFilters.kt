package org.babyfish.jimmer.sql.kt.filter

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KFilters {

    fun <T: Any> getFilter(type: KClass<T>): KFilter<T>?

    fun <T: Any> getTargetFilter(prop: KProperty1<*, T?>): KFilter<T>?

    fun <T: Any> getParameterizedFilter(type: KClass<T>): KFilter.Parameterized<T>?

    fun <T: Any> getParameterizedTargetFilter(prop: KProperty1<*, T?>): KFilter.Parameterized<T>?
}