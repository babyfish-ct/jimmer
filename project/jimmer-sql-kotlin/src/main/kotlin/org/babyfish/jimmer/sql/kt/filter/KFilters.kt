package org.babyfish.jimmer.sql.kt.filter

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KFilters {

    fun <T: Any> getFilter(type: KClass<T>): KFilter<T>?

    fun <T: Any> getTargetFilter(prop: KProperty1<*, T?>): KFilter<T>?

    fun <T: Any> getCacheableFilter(type: KClass<T>): KCacheableFilter<T>?

    fun <T: Any> getCacheableTargetFilter(prop: KProperty1<*, T?>): KCacheableFilter<T>?
}