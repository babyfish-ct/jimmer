package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.association.Association
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KNonNullTableEx<E: Any> : KNonNullTable<E>, KTableEx<E> {

    override fun <X: Any> join(prop: String): KNonNullTableEx<X>
    override fun <X: Any> joinReference(prop: KProperty1<E, X?>): KNonNullTableEx<X>
    override fun <X: Any> joinList(prop: KProperty1<E, List<X>>): KNonNullTableEx<X>

    override fun <X: Any> inverseJoin(backProp: ImmutableProp): KNonNullTableEx<X>
    override fun <X: Any> inverseJoinReference(backProp: KProperty1<X, E?>): KNonNullTableEx<X>
    override fun <X: Any> inverseJoinList(backProp: KProperty1<X, List<E>>): KNonNullTableEx<X>
}

val <S: Any, T: Any> KNonNullTableEx<Association<S, T>>.source: KNonNullTableEx<S>
    get() = join("source")

val <S: Any, T: Any> KNonNullTableEx<Association<S, T>>.target: KNonNullTableEx<T>
    get() = join("target")
