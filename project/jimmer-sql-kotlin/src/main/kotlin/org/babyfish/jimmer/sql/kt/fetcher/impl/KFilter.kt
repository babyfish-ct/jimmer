package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.kt.fetcher.KFilterDsl

fun interface KFilter<E: Any> {

    fun KFilterDsl<E>.apply()
}