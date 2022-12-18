package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.ast.tuple.*
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal class KMutableRootQueryImpl<E: Any>(
    private val javaQuery: MutableRootQueryImpl<Table<E>>
) : KMutableRootQuery<E> {

    override val table: KNonNullTable<E> =
        KNonNullTableExImpl(javaQuery.getTable())

    override fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        javaQuery.where(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun orderBy(vararg expressions: KExpression<*>?) {
        javaQuery.orderBy(*expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderByIf(condition: Boolean, vararg expressions: KExpression<*>?) {
        javaQuery.orderByIf(condition, *expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderBy(vararg orders: Order?) {
        javaQuery.orderBy(*orders)
    }

    override fun orderByIf(condition: Boolean, vararg orders: Order?) {
        javaQuery.orderByIf(condition, *orders)
    }

    @Suppress("UNCHECKED_CAST")
    override fun groupBy(vararg expressions: KExpression<*>) {
        javaQuery.groupBy(*expressions.map { it as Expression<*>}.toTypedArray())
    }

    override fun having(vararg predicates: KNonNullExpression<Boolean>?) {
        javaQuery.having(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun <T> select(selection: Selection<T>): KConfigurableRootQuery<E, T> =
        KConfigurableRootQueryImpl(javaQuery.select(selection))

    override fun <T1, T2> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>
    ): KConfigurableRootQuery<E, Tuple2<T1, T2>> =
        KConfigurableRootQueryImpl(javaQuery.select(selection1, selection2))

    override fun <T1, T2, T3> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>
    ): KConfigurableRootQuery<E, Tuple3<T1, T2, T3>> =
        KConfigurableRootQueryImpl(
            javaQuery.select(
                selection1,
                selection2,
                selection3
            )
        )

    override fun <T1, T2, T3, T4> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>
    ): KConfigurableRootQuery<E, Tuple4<T1, T2, T3, T4>> =
        KConfigurableRootQueryImpl(
            javaQuery.select(
                selection1,
                selection2,
                selection3,
                selection4
            )
        )

    override fun <T1, T2, T3, T4, T5> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>
    ): KConfigurableRootQuery<E, Tuple5<T1, T2, T3, T4, T5>> =
        KConfigurableRootQueryImpl(
            javaQuery.select(
                selection1,
                selection2,
                selection3,
                selection4,
                selection5
            )
        )

    override fun <T1, T2, T3, T4, T5, T6> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>
    ): KConfigurableRootQuery<E, Tuple6<T1, T2, T3, T4, T5, T6>> =
        KConfigurableRootQueryImpl(
            javaQuery.select(
                selection1,
                selection2,
                selection3,
                selection4,
                selection5,
                selection6
            )
        )

    override fun <T1, T2, T3, T4, T5, T6, T7> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>
    ): KConfigurableRootQuery<E, Tuple7<T1, T2, T3, T4, T5, T6, T7>> =
        KConfigurableRootQueryImpl(
            javaQuery.select(
                selection1,
                selection2,
                selection3,
                selection4,
                selection5,
                selection6,
                selection7
            )
        )

    override fun <T1, T2, T3, T4, T5, T6, T7, T8> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>,
        selection8: Selection<T8>
    ): KConfigurableRootQuery<E, Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> =
        KConfigurableRootQueryImpl(
            javaQuery.select(
                selection1,
                selection2,
                selection3,
                selection4,
                selection5,
                selection6,
                selection7,
                selection8
            )
        )

    override fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>,
        selection8: Selection<T8>,
        selection9: Selection<T9>
    ): KConfigurableRootQuery<E, Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> =
        KConfigurableRootQueryImpl(
            javaQuery.select(
                selection1,
                selection2,
                selection3,
                selection4,
                selection5,
                selection6,
                selection7,
                selection8,
                selection9
            )
        )

    override val subQueries: KSubQueries<E> =
        KSubQueriesImpl(javaQuery)

    override val wildSubQueries: KWildSubQueries<E> =
        KWildSubQueriesImpl(javaQuery)
}