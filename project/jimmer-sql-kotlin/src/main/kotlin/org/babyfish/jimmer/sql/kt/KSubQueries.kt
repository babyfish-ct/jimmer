package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableTypedSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KSubQueries {

    fun <E: Any, R> forEntity(
        entityType: KClass<E>,
        block: KMutableSubQuery<E>.() -> KConfigurableTypedSubQuery<R>
    ): KConfigurableTypedSubQuery<R>

    fun <S: Any, T: Any, R> forReference(
        prop: KProperty1<S, R?>,
        block: KMutableSubQuery<Association<S, T>>.() -> KConfigurableTypedSubQuery<R>
    ): KConfigurableTypedSubQuery<R>

    fun <S: Any, T: Any, R> forList(
        prop: KProperty1<S, List<R>>,
        block: KMutableSubQuery<Association<S, T>>.() -> KConfigurableTypedSubQuery<R>
    ): KConfigurableTypedSubQuery<R>
}