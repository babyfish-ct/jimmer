package org.babyfish.jimmer.sql.kt.fetcher

interface KListFieldDsl<E: Any> : KFieldDsl<E> {

    fun limit(limit: Int, offset: Int = 0)
}