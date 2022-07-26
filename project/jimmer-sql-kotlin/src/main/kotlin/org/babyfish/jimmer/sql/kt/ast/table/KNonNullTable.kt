package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.ast.Selection
import kotlin.reflect.KClass

interface KNonNullTable<E: Any> : KTable<E>, Selection<E> {
    override fun <X: Any> join(prop: String): KNonNullTable<X>
    override fun <X: Any> inverseJoin(targetType: KClass<X>, backProp: String): KNonNullTable<X>
}

val <S: Any, T: Any> KNonNullTable<Association<S, T>>.source: KNonNullTable<S>
    get() = join("source")

val <S: Any, T: Any> KNonNullTable<Association<S, T>>.target: KNonNullTable<T>
    get() = join("target")
