package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableSelectionLayout
import org.babyfish.jimmer.sql.kt.ast.query.KTypedBaseQuery
import org.babyfish.jimmer.sql.kt.ast.table.KBaseTableSymbol
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.impl.AbstractKBaseTable

internal class KMergedBaseQueryImpl<T : KNonNullBaseTable<*>>(
    first: KTypedBaseQuery<T>,
    private val javaBaseQuery: MergedBaseQueryImpl<*>
) : KTypedBaseQuery<T>, Ast {

    private val selectionLayout = selectionLayout(first)

    override fun asBaseTable(): KBaseTableSymbol<T> =
        asBaseTable(false)

    override fun asCteBaseTable(): KBaseTableSymbol<T> =
        asBaseTable(true)

    @Suppress("UNCHECKED_CAST")
    private fun asBaseTable(cte: Boolean): KBaseTableSymbol<T> {
        val symbol = (javaBaseQuery as TypedBaseQueryImplementor<*>)
            .asBaseTableSymbol(selectionLayout, cte)
        return KBaseTableSymbol(AbstractKBaseTable.nonNull(symbol) as T)
    }

    override fun accept(visitor: AstVisitor) {
        javaBaseQuery.accept(visitor)
    }

    override fun renderTo(builder: AbstractSqlBuilder<*>) {
        javaBaseQuery.renderTo(builder)
    }

    override fun hasVirtualPredicate(): Boolean =
        javaBaseQuery.hasVirtualPredicate()

    override fun resolveVirtualPredicate(ctx: AstContext?): Ast {
        javaBaseQuery.resolveVirtualPredicate(ctx)
        return this
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun javaBaseQuery(query: KTypedBaseQuery<*>): TypedBaseQuery<BaseTable> =
            if (query is KMergedBaseQueryImpl<*>) {
                query.javaBaseQuery
            } else {
                (query as AbstractKConfigurableBaseQueryImpl<*>).javaBaseQuery
            } as TypedBaseQuery<BaseTable>

        fun javaClient(query: KTypedBaseQuery<*>): JSqlClient =
            if (query is KMergedBaseQueryImpl<*>) {
                query.javaBaseQuery.sqlClient
            } else {
                ((query as AbstractKConfigurableBaseQueryImpl<*>).javaBaseQuery as ConfigurableBaseQueryImpl<*>).sqlClient
            }

        private fun selectionLayout(query: KTypedBaseQuery<*>): BaseTableSelectionLayout =
            if (query is KMergedBaseQueryImpl<*>) {
                query.selectionLayout
            } else {
                (query as AbstractKConfigurableBaseQueryImpl<*>).selectionLayout
            }
    }
}
