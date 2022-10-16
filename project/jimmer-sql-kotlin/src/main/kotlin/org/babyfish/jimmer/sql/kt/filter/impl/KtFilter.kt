package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl
import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.filter.impl.AbstractFilterArgsImpl
import org.babyfish.jimmer.sql.kt.ast.table.impl.KTableExImpl
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs

internal open class KtFilter<E: Any>(
    protected val javaFilter: Filter<Props>
) : KFilter<E> {

    override fun filter(args: KFilterArgs<E>) {
        val javaQuery = (args as KFilterArgsImpl<E>).javaQuery
        val javaArgs = JavaArgs(javaQuery, (args.table as KTableExImpl<E>).javaTable)
        javaFilter.filter(javaArgs)
    }

    private class JavaArgs(
        javaQuery: AbstractMutableQueryImpl,
        private val javaProps: Props
    ) : AbstractFilterArgsImpl<Props>(javaQuery) {

        override fun getTable(): Props = javaProps
    }
}