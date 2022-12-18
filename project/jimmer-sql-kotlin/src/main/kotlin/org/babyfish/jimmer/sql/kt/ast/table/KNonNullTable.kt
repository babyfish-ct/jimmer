package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KNonNullTable<E: Any> : KTable<E>, KNonNullProps<E>, Selection<E> {

    fun fetch(fetcher: Fetcher<E>?): Selection<E>

    override fun asTableEx(): KNonNullTableEx<E>
}

val <S: Any, T: Any> KNonNullTable<Association<S, T>>.source: KNonNullTable<S>
    get() = join("source")

val <S: Any, T: Any> KNonNullTable<Association<S, T>>.target: KNonNullTable<T>
    get() = join("target")
