package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KNonNullTable<E: Any> : KTable<E>, Selection<E> {

    override fun <X: Any> join(prop: String): KNonNullTable<X>
    override fun <X: Any> joinReference(prop: KProperty1<E, X?>): KNonNullTable<X>
    override fun <X: Any> joinList(prop: KProperty1<E, List<X>>): KNonNullTable<X>

    override fun <X: Any> inverseJoin(backProp: ImmutableProp): KNonNullTable<X>
    override fun <X: Any> inverseJoinReference(backProp: KProperty1<X, E?>): KNonNullTable<X>
    override fun <X: Any> inverseJoinList(backProp: KProperty1<X, List<E>>): KNonNullTable<X>

    fun fetch(fetcher: Fetcher<E>): Selection<E>

    override fun asTableEx(): KNonNullTableEx<E>
}

val <S: Any, T: Any> KNonNullTable<Association<S, T>>.source: KNonNullTable<S>
    get() = join("source")

val <S: Any, T: Any> KNonNullTable<Association<S, T>>.target: KNonNullTable<T>
    get() = join("target")
