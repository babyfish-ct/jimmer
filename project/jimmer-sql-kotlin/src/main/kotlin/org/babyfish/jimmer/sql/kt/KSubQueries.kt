package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KSubQueries<P: Any> {

    fun <E: Any, R, SQ: KConfigurableSubQuery<R>> forEntity(
        entityType: KClass<E>,
        block: KMutableSubQuery<P, E>.() -> SQ
    ): SQ

    fun <S: Any, T: Any, R, SQ: KConfigurableSubQuery<R>> forReference(
        prop: KProperty1<S, R?>,
        block: KMutableSubQuery<P, Association<S, T>>.() -> SQ
    ): SQ

    fun <S: Any, T: Any, R, SQ: KConfigurableSubQuery<R>> forList(
        prop: KProperty1<S, List<R>>,
        block: KMutableSubQuery<P, Association<S, T>>.() -> SQ
    ): SQ
}