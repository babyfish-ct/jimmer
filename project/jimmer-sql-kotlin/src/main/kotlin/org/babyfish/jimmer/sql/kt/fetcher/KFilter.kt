package org.babyfish.jimmer.sql.kt.fetcher

fun interface KFilter<E: Any> {

    fun KFilterDsl<E>.apply()
}