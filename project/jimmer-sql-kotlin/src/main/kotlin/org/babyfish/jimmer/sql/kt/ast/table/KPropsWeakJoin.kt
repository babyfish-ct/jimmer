package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.Predicate
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl
import org.babyfish.jimmer.sql.ast.impl.table.KWeakJoinImplementor
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.table.BaseTable
import org.babyfish.jimmer.sql.ast.table.WeakJoin
import org.babyfish.jimmer.sql.ast.table.spi.TableLike
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy
import org.babyfish.jimmer.sql.ast.table.spi.UsingWeakJoinMetadataParser
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.table.impl.AbstractKBaseTableImpl
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.ast.table.impl.KPropsWeakJoinMetadataParser
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

@UsingWeakJoinMetadataParser(KPropsWeakJoinMetadataParser::class)
abstract class KPropsWeakJoin<SP: KPropsLike, TP: KPropsLike>:
    WeakJoin<TableLike<*>, TableLike<*>>,
    KWeakJoinImplementor<TableLike<*>, TableLike<*>> {

    @Suppress("UNCHECKED_CAST")
    final override fun on(
        source: TableLike<TableLike<*>>,
        target: TableLike<TableLike<*>>,
        statement: AbstractMutableStatementImpl
    ): Predicate? {

        val st = if (source is BaseTable) {
            AbstractKBaseTableImpl.nonNull(source)
        } else if (source is UntypedJoinDisabledTableProxy<*>) {
            KNonNullTableExImpl(source.__unwrap(), JOIN_ERROR_REASON)
        } else {
            KNonNullTableExImpl(source as TableImplementor<*>, JOIN_ERROR_REASON)
        }
        val tt = if (target is BaseTable)  {
            AbstractKBaseTableImpl.nonNull(target)
        } else if (target is UntypedJoinDisabledTableProxy<*>) {
            KNonNullTableExImpl(target.__unwrap(), JOIN_ERROR_REASON)
        } else {
            KNonNullTableExImpl(target as TableImplementor<*>, JOIN_ERROR_REASON)
        }
        return on(
            st as SP,
            tt as TP,
            ContextImpl(statement, st, tt)
        )?.toJavaPredicate()
    }

    open fun on(
        source: SP,
        target: TP
    ): KNonNullExpression<Boolean>? = null

    open fun on(
        source: SP,
        target: TP,
        ctx: Context<SP, TP>
    ): KNonNullExpression<Boolean>? =
        on(source, target)

    interface Context<SP: KPropsLike, TP: KPropsLike> {
        val sourceSubQueries: KSubQueries<SP>
        val sourceWildSubQueries: KWildSubQueries<SP>
        val targetSubQueries: KSubQueries<TP>
        val targetWildSubQueries: KWildSubQueries<TP>
    }

    final override fun on(source: TableLike<*>, target: TableLike<*>): Predicate {
        throw UnsupportedOperationException(
            "The method with 2 arguments is forbidden"
        )
    }

    private class ContextImpl<SP: KPropsLike, TP: KPropsLike>(
        statement: AbstractMutableStatementImpl,
        source: SP,
        target: TP
    ) : Context<SP, TP> {
        override val sourceSubQueries: KSubQueries<SP> =
            KSubQueriesImpl(statement, source)
        override val sourceWildSubQueries: KWildSubQueries<SP> =
            KWildSubQueriesImpl(statement, source)
        override val targetSubQueries: KSubQueries<TP> =
            KSubQueriesImpl(statement, target)
        override val targetWildSubQueries: KWildSubQueries<TP> =
            KWildSubQueriesImpl(statement, target)
    }

    companion object {
        private val JOIN_ERROR_REASON = "it is forbidden in the implementation of \"" +
            KPropsWeakJoin::class.java.name +
            "\""
    }
}