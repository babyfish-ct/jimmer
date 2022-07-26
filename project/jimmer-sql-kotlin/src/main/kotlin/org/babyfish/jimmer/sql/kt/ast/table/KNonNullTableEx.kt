package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.association.Association
import kotlin.reflect.KClass

interface KNonNullTableEx<E: Any> : KNonNullTable<E>, KTableEx<E> {
    override fun <X: Any> join(prop: String): KNonNullTableEx<X>
    override fun <X: Any> inverseJoin(targetType: KClass<X>, backProp: String): KNonNullTableEx<X>
}

val <S: Any, T: Any> KNonNullTableEx<Association<S, T>>.source: KNonNullTableEx<S>
    get() = join("source")

val <S: Any, T: Any> KNonNullTableEx<Association<S, T>>.target: KNonNullTableEx<T>
    get() = join("target")
