package org.babyfish.jimmer.sql.kt.fetcher

import org.babyfish.jimmer.sql.kt.fetcher.impl.KFilter

interface KFieldDsl<E: Any> {

    fun batch(size: Int)

    fun filter(filter: KFilter<E>)
}