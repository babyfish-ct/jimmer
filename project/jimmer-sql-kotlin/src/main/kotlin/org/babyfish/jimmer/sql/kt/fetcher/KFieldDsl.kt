package org.babyfish.jimmer.sql.kt.fetcher

interface KFieldDsl<E: Any> {

    fun batch(size: Int)

    fun filter(filter: KFieldFilterDsl<E>.() -> Unit)
}