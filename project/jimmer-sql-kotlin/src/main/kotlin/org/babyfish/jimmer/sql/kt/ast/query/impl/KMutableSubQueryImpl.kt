package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.query.MutableSubQueryImpl
import org.babyfish.jimmer.sql.ast.query.*
import org.babyfish.jimmer.sql.ast.tuple.*
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KNullableTable
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal class KMutableSubQueryImpl<P: Any, E: Any>(
    private val javaSubQuery: MutableSubQueryImpl
) : KMutableSubQuery<P, E> {

    override val table: KNonNullTableEx<E> =
        KNonNullTableExImpl(javaSubQuery.getTable())

    override val parentTable: KNonNullTableEx<P> =
        KNonNullTableExImpl(javaSubQuery.parent.getTable())

    override fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        javaSubQuery.where(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun orderBy(vararg expressions: KExpression<*>?) {
        javaSubQuery.orderBy(*expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderByIf(condition: Boolean, vararg expressions: KExpression<*>?) {
        javaSubQuery.orderByIf(condition, *expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderBy(vararg orders: Order?) {
        javaSubQuery.orderBy(*orders)
    }

    override fun orderByIf(condition: Boolean, vararg orders: Order?) {
        javaSubQuery.orderByIf(condition, *orders)
    }

    override fun groupBy(vararg expressions: KExpression<*>) {
        javaSubQuery.groupBy(*expressions.map { it as Expression<*>}.toTypedArray())
    }

    override fun having(vararg predicates: KNonNullExpression<Boolean>?) {
        javaSubQuery.having(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun <T : Any> select(
        expression: KNonNullExpression<T>
    ): KConfigurableSubQuery.NonNull<T> =
        KConfigurableSubQueryImpl.NonNull(javaSubQuery.select(expression))

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> select(expression: KNullableExpression<T>): KConfigurableSubQuery.Nullable<T> =
        KConfigurableSubQueryImpl.Nullable(javaSubQuery.select(expression) as ConfigurableSubQuery<T>)

    override fun <T : Any> select(table: KNonNullTable<T>): KConfigurableSubQuery.NonNull<T> =
        KConfigurableSubQueryImpl.NonNull(javaSubQuery.select(table))

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> select(table: KNullableTable<T>): KConfigurableSubQuery.Nullable<T> =
        KConfigurableSubQueryImpl.Nullable(javaSubQuery.select(table) as ConfigurableSubQuery<T>)

    override fun <T1, T2> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>
    ): KConfigurableSubQuery.NonNull<Tuple2<T1, T2>> =
        KConfigurableSubQueryImpl.NonNull(
            javaSubQuery.select(
                selection1,
                selection2
            )
        )

    override fun <T1, T2, T3> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>
    ): KConfigurableSubQuery.NonNull<Tuple3<T1, T2, T3>> =
        KConfigurableSubQueryImpl.NonNull(
            javaSubQuery.select(
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
    ): KConfigurableSubQuery.NonNull<Tuple4<T1, T2, T3, T4>> =
        KConfigurableSubQueryImpl.NonNull(
            javaSubQuery.select(
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
    ): KConfigurableSubQuery.NonNull<Tuple5<T1, T2, T3, T4, T5>> =
        KConfigurableSubQueryImpl.NonNull(
            javaSubQuery.select(
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
    ): KConfigurableSubQuery.NonNull<Tuple6<T1, T2, T3, T4, T5, T6>> =
        KConfigurableSubQueryImpl.NonNull(
            javaSubQuery.select(
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
    ): KConfigurableSubQuery.NonNull<Tuple7<T1, T2, T3, T4, T5, T6, T7>> =
        KConfigurableSubQueryImpl.NonNull(
            javaSubQuery.select(
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
    ): KConfigurableSubQuery.NonNull<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> =
        KConfigurableSubQueryImpl.NonNull(
            javaSubQuery.select(
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
    ): KConfigurableSubQuery.NonNull<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> =
        KConfigurableSubQueryImpl.NonNull(
            javaSubQuery.select(
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
        KSubQueriesImpl(javaSubQuery)

    override val wildSubQueries: KWildSubQueries<E> =
        KWildSubQueriesImpl(javaSubQuery)
}