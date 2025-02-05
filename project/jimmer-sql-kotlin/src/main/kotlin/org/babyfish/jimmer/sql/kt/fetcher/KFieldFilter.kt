package org.babyfish.jimmer.sql.kt.fetcher

interface KFieldFilter<E: Any> {

    fun KFieldFilterDsl<E>.applyTo()
}