package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.TypeMatchMode
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl
import org.babyfish.jimmer.sql.ast.tuple.*
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NonNullPropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullablePropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdateReturning
import org.babyfish.jimmer.sql.kt.ast.query.Where
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.impl.KExecutableImpl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl
import org.babyfish.jimmer.sql.runtime.TupleMapper

internal class KMutableUpdateImpl<E : Any>(
    private val javaUpdate: MutableUpdateImpl
) : KMutableUpdateReturning<E> {

    override val table: KNonNullTableEx<E> =
        KNonNullTableExImpl(javaUpdate.getTable())

    override val where: Where by lazy {
        Where(this)
    }

    override fun setTypeMatchMode(mode: TypeMatchMode) {
        javaUpdate.setTypeMatchMode(mode)
    }

    override fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        javaUpdate.where(*predicates.map { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun where(block: () -> KNonNullExpression<Boolean>?) {
        where(block())
    }

    @Suppress("UNCHECKED_CAST")
    override fun <X : Any> set(path: KNonNullPropExpression<X>, value: KNonNullExpression<X>) {
        javaUpdate.set(
            (path as NonNullPropExpressionImpl<X>).javaPropExpression,
            value as Expression<X>
        )
    }

    override fun <X : Any> set(path: KNonNullPropExpression<X>, value: X) {
        javaUpdate.set(
            (path as NonNullPropExpressionImpl<X>).javaPropExpression,
            Expression.value(value)
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <X : Any> set(path: KNullablePropExpression<X>, value: KExpression<X>) {
        javaUpdate.set(
            (path as NullablePropExpressionImpl<X>).javaPropExpression,
            value as Expression<X>
        )
    }

    override fun <X : Any> set(path: KNullableExpression<X>, value: X?) {
        val javaPropExpression = (path as NullablePropExpressionImpl<X>).javaPropExpression
        javaUpdate.set(
            javaPropExpression,
            if (value !== null) {
                Expression.any().value(value)
            } else {
                Expression.nullValue(javaPropExpression.type)
            }
        )
    }

    override fun <T> returning(selection: Selection<T>): KExecutable<List<T>> =
        KExecutableImpl(javaUpdate.returning(selection))

    override fun <T1, T2> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>
    ): KExecutable<List<Tuple2<T1, T2>>> =
        KExecutableImpl(javaUpdate.returning(selection1, selection2))

    override fun <T1, T2, T3> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>
    ): KExecutable<List<Tuple3<T1, T2, T3>>> =
        KExecutableImpl(javaUpdate.returning(selection1, selection2, selection3))

    override fun <T1, T2, T3, T4> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>
    ): KExecutable<List<Tuple4<T1, T2, T3, T4>>> =
        KExecutableImpl(javaUpdate.returning(selection1, selection2, selection3, selection4))

    override fun <T1, T2, T3, T4, T5> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>
    ): KExecutable<List<Tuple5<T1, T2, T3, T4, T5>>> =
        KExecutableImpl(javaUpdate.returning(selection1, selection2, selection3, selection4, selection5))

    override fun <T1, T2, T3, T4, T5, T6> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>
    ): KExecutable<List<Tuple6<T1, T2, T3, T4, T5, T6>>> =
        KExecutableImpl(javaUpdate.returning(selection1, selection2, selection3, selection4, selection5, selection6))

    override fun <T1, T2, T3, T4, T5, T6, T7> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>
    ): KExecutable<List<Tuple7<T1, T2, T3, T4, T5, T6, T7>>> =
        KExecutableImpl(
            javaUpdate.returning(
                selection1,
                selection2,
                selection3,
                selection4,
                selection5,
                selection6,
                selection7
            )
        )

    override fun <T1, T2, T3, T4, T5, T6, T7, T8> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>,
        selection8: Selection<T8>
    ): KExecutable<List<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>> =
        KExecutableImpl(
            javaUpdate.returning(
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

    override fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> returning(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>,
        selection4: Selection<T4>,
        selection5: Selection<T5>,
        selection6: Selection<T6>,
        selection7: Selection<T7>,
        selection8: Selection<T8>,
        selection9: Selection<T9>
    ): KExecutable<List<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>>> =
        KExecutableImpl(
            javaUpdate.returning(
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

    override fun <T> returning(mapper: TupleMapper<T>): KExecutable<List<T>> =
        KExecutableImpl(javaUpdate.returning(mapper))

    override val subQueries: KSubQueries<KNonNullTableEx<E>> =
        KSubQueriesImpl(javaUpdate, table)

    override val wildSubQueries: KWildSubQueries<KNonNullTableEx<E>> =
        KWildSubQueriesImpl(javaUpdate, table)
}
