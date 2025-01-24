package org.babyfish.jimmer.sql.kt.fetcher

import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType

interface KReferenceFieldDsl<E: Any> : KFieldDsl<E> {

    fun fetchType(fetchType: ReferenceFetchType)
}