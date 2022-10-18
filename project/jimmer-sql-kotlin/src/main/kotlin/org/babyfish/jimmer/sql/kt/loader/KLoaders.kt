package org.babyfish.jimmer.sql.kt.loader

import kotlin.reflect.KProperty1

interface KLoaders {

    fun <S: Any, T: Any> batchLoad(prop: KProperty1<S, T?>, sources: Collection<S>): Map<S, T>

    fun <S: Any, T: Any> value(prop: KProperty1<S, T>): KValueLoader<S, T>

    fun <S: Any, T: Any> reference(prop: KProperty1<S, T?>): KReferenceLoader<S, T>

    fun <S: Any, T: Any> list(prop: KProperty1<S, List<T>>): KListLoader<S, T>
}