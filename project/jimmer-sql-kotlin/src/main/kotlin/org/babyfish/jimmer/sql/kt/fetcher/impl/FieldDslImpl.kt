package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType
import org.babyfish.jimmer.sql.fetcher.ReferenceFieldConfig
import org.babyfish.jimmer.sql.kt.fetcher.KFieldFilterDsl
import org.babyfish.jimmer.sql.kt.fetcher.KRecursiveListFieldDsl
import org.babyfish.jimmer.sql.kt.fetcher.KRecursiveReferenceFieldDsl

internal class FieldDslImpl<E: Any>(
    private val fieldConfig: RecursiveListFieldConfig<E, Table<E>>
) : KRecursiveReferenceFieldDsl<E>, KRecursiveListFieldDsl<E> {

    override fun batch(size: Int) {
        fieldConfig.batch(size)
    }

    override fun filter(filter: KFieldFilterDsl<E>.() -> Unit) {
        fieldConfig.filter(JavaFieldFilter(filter))
    }

    override fun limit(limit: Int, offset: Int) {
        fieldConfig.limit(limit, offset)
    }

    override fun fetchType(fetchType: ReferenceFetchType) {
        (fieldConfig as ReferenceFieldConfig<*, *>).fetchType(fetchType)
    }

    override fun depth(depth: Int) {
        fieldConfig.depth(depth)
    }

    override fun recursive(block: RecursionStrategy.Args<E>.() -> Boolean) {
        fieldConfig.recursive(block)
    }
}