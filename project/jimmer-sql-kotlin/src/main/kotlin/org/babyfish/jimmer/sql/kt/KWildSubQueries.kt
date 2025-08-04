package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KWildSubQueries<P: KPropsLike> {

    fun <E: Any> forEntity(
        entityType: KClass<E>,
        block: KMutableSubQuery<P, KNonNullTableEx<E>>.() -> Unit
    ): KMutableSubQuery<P, KNonNullTableEx<E>>

    fun <S: Any, T: Any> forReference(
        prop: KProperty1<S, T?>,
        block: KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>>.() -> Unit
    ): KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>>

    fun <S: Any, T: Any> forList(
        prop: KProperty1<S, List<T>>,
        block: KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>>.() -> Unit
    ): KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>>
}