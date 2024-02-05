package org.babyfish.jimmer.sql.kt.fetcher

import org.babyfish.jimmer.kt.DslScope

@DslScope
interface KFieldDsl<E: Any> {

    fun batch(size: Int)

    fun filter(filter: KFieldFilterDsl<E>.() -> Unit)
}