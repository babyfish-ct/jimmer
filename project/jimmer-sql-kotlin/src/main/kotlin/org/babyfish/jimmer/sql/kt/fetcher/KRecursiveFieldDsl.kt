package org.babyfish.jimmer.sql.kt.fetcher

import org.babyfish.jimmer.sql.fetcher.RecursionStrategy

interface KRecursiveFieldDsl<E: Any> : KFieldDsl<E> {

    fun depth(depth: Int)

    fun recursive(block: RecursionStrategy.Args<E>.() -> Boolean)
}