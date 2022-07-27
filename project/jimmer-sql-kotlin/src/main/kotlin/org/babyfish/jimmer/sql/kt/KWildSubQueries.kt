package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KWildSubQueries<P: Any> {

    fun <E: Any> forEntity(
        entityType: KClass<E>,
        block: KMutableSubQuery<P, E>.() -> Unit
    ): KMutableSubQuery<P, E>

    fun <S: Any, T: Any> forReference(
        prop: KProperty1<S, T?>,
        block: KMutableSubQuery<P, Association<S, T>>.() -> Unit
    ): KMutableSubQuery<P, Association<S, T>>

    fun <S: Any, T: Any> forList(
        prop: KProperty1<S, List<T>>,
        block: KMutableSubQuery<P, Association<S, T>>.() -> Unit
    ): KMutableSubQuery<P, Association<S, T>>
}