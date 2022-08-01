package org.babyfish.jimmer.sql.kt.fetcher

import org.babyfish.jimmer.sql.cache.CacheFilter

fun interface KFilter<E: Any> : CacheFilter {

    fun KFilterDsl<E>.apply()
}