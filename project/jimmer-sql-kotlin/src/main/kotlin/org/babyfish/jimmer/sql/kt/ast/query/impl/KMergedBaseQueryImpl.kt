package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.kt.ast.query.KTypedBaseQuery
import org.babyfish.jimmer.sql.kt.ast.table.KBaseTableSymbol
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable

internal class KMergedBaseQueryImpl<T : KNonNullBaseTable<*>>(
    private val first: KTypedBaseQuery<T>,
    private val javaBaseQuery: MergedBaseQueryImpl<*>
) : KTypedBaseQuery<T>, Ast {

    override fun asBaseTable(): KBaseTableSymbol<T> =
        first.asBaseTable()

    override fun asCteBaseTable(): KBaseTableSymbol<T> =
        first.asCteBaseTable()

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
    }
}