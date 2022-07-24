package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KQueries {

    fun <E: Any, R> forEntity(
        entityType: KClass<E>,
        block: KMutableRootQuery<E>.() -> KConfigurableRootQuery<E, R>
    ): KConfigurableRootQuery<E, R>

    fun <S: Any, T: Any, R> forReference(
        prop: KProperty1<S, R?>,
        block: KMutableRootQuery<Association<S, T>>.() -> KConfigurableRootQuery<Association<S, T>, R>
    ): KConfigurableRootQuery<Association<S, T>, R>

    fun <S: Any, T: Any, R> forList(
        prop: KProperty1<S, List<R>>,
        block: KMutableRootQuery<Association<S, T>>.() -> KConfigurableRootQuery<Association<S, T>, R>
    ): KConfigurableRootQuery<Association<S, T>, R>
}