package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableTypedSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KWildSubQueries {

    fun <E: Any> forEntity(
        entityType: KClass<E>,
        block: KMutableSubQuery<E>.() -> Unit
    ): KMutableSubQuery<E>

    fun <S: Any, T: Any, R> forReference(
        prop: KProperty1<S, R?>,
        block: KMutableSubQuery<Association<S, T>>.() -> Unit
    ): KMutableSubQuery<Association<S, T>>

    fun <S: Any, T: Any, R> forList(
        prop: KProperty1<S, List<R>>,
        block: KMutableSubQuery<Association<S, T>>.() -> Unit
    ): KMutableSubQuery<Association<S, T>>
}