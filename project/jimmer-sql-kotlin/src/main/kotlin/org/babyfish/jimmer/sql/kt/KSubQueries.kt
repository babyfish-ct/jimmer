package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KSubQueries<P: KPropsLike> {

    fun <E: Any, R, SQ: KConfigurableSubQuery<R>> forEntity(
        entityType: KClass<E>,
        block: KMutableSubQuery<P, KNonNullTableEx<E>>.() -> SQ
    ): SQ

    fun <S: Any, T: Any, R, SQ: KConfigurableSubQuery<R>> forReference(
        prop: KProperty1<S, T?>,
        block: KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>>.() -> SQ
    ): SQ

    fun <S: Any, T: Any, R, SQ: KConfigurableSubQuery<R>> forList(
        prop: KProperty1<S, List<T>>,
        block: KMutableSubQuery<P, KNonNullTableEx<Association<S, T>>>.() -> SQ
    ): SQ
}