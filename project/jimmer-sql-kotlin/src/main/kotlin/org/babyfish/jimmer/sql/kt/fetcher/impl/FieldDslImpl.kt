package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig
import org.babyfish.jimmer.sql.kt.fetcher.KFilter
import org.babyfish.jimmer.sql.kt.fetcher.KFilterDsl
import org.babyfish.jimmer.sql.kt.fetcher.KRecursiveListFieldDsl

internal class FieldDslImpl<E: Any>(
    private val fieldConfig: RecursiveListFieldConfig<E, Table<E>>
) : KRecursiveListFieldDsl<E> {

    override fun batch(size: Int) {
        fieldConfig.batch(size)
    }

    override fun filter(filter: KFilterDsl<E>.() -> Unit) {
        fieldConfig.filter(LambdaFieldFilterWrapper(filter))
    }

    override fun filter(filter: KFilter<E>) {
        fieldConfig.filter(KtFieldFilterWrapper(filter))
    }

    override fun limit(limit: Int, offset: Int) {
        fieldConfig.limit(limit, offset)
    }

    override fun depth(depth: Int) {
        fieldConfig.depth(depth)
    }

    override fun recursive(block: (RecursionStrategy.Args<E>.() -> Boolean)?) {
        if (block !== null) {
            fieldConfig.recursive(block)
        } else {
            fieldConfig.recursive()
        }
    }
}