package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.Static
import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher
import kotlin.reflect.KClass

interface KNonNullTable<E: Any> : KTable<E>, KNonNullProps<E>, Selection<E> {

    fun fetch(fetcher: Fetcher<E>?): Selection<E>

    fun <S: Static<E>> fetch(staticType: KClass<S>): Selection<S>

    override fun asTableEx(): KNonNullTableEx<E>
}

val <S: Any, T: Any> KNonNullTable<Association<S, T>>.source: KNonNullTable<S>
    get() = join("source")

val <S: Any, T: Any> KNonNullTable<Association<S, T>>.target: KNonNullTable<T>
    get() = join("target")
