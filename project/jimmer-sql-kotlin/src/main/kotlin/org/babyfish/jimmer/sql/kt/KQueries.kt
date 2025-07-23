package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KQueries {

    fun <E: Any, R> forEntity(
        entityType: KClass<E>,
        block: KMutableRootQuery.ForEntity<E>.() -> KConfigurableRootQuery<KNonNullTable<E>, R>
    ): KConfigurableRootQuery<KNonNullTable<E>, R>

    fun <S: Any, T: Any, R> forReference(
        prop: KProperty1<S, T?>,
        block: KMutableRootQuery<KNonNullTable<Association<S, T>>>.() -> KConfigurableRootQuery<KNonNullTable<Association<S, T>>, R>
    ): KConfigurableRootQuery<KNonNullTable<Association<S, T>>, R>

    fun <S: Any, T: Any, R> forList(
        prop: KProperty1<S, List<T>>,
        block: KMutableRootQuery<KNonNullTable<Association<S, T>>>.() -> KConfigurableRootQuery<KNonNullTable<Association<S, T>>, R>
    ): KConfigurableRootQuery<KNonNullTable<Association<S, T>>, R>
}