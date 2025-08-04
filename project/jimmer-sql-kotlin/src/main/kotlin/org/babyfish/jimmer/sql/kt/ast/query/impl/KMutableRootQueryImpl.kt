package org.babyfish.jimmer.sql.kt.ast.query.impl

import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.sql.association.Association
import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl
import org.babyfish.jimmer.sql.ast.impl.query.MutableStatementImplementor
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.ast.query.specification.PredicateApplier
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.ast.table.spi.TableLike
import org.babyfish.jimmer.sql.ast.tuple.*
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.Where
import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecificationArgs
import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecification
import org.babyfish.jimmer.sql.kt.ast.table.KBaseTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KNullableTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNullableTableExImpl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal abstract class KMutableRootQueryImpl<P: KPropsLike>(
    protected val javaQuery: MutableRootQueryImpl<TableLike<*>>
) : KMutableRootQuery<P>, MutableStatementImplementor {

    override val where: Where by lazy {
        Where(this)
    }

    override fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        javaQuery.where(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun where(block: () -> KNonNullPropExpression<Boolean>?) {
        where(block())
    }

    override fun orderBy(vararg expressions: KExpression<*>?) {
        javaQuery.orderBy(*expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderBy(vararg orders: Order?) {
        javaQuery.orderBy(*orders)
    }

    override fun orderBy(orders: List<Order?>) {
        javaQuery.orderBy(orders)
    }

    override fun groupBy(vararg expressions: KExpression<*>) {
        javaQuery.groupBy(*expressions.map { it as Expression<*>}.toTypedArray())
    }

    override fun having(vararg predicates: KNonNullExpression<Boolean>?) {
        javaQuery.having(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun <T> select(selection: Selection<T>): KConfigurableRootQuery<P, T> =
        KConfigurableRootQueryImpl(javaQuery.select(selection))

    override fun <T1, T2> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>
    ): KConfigurableRootQuery<P, Tuple2<T1, T2>> =
        KConfigurableRootQueryImpl(javaQuery.select(selection1, selection2))

    override fun <T1, T2, T3> select(
        selection1: Selection<T1>,
        selection2: Selection<T2>,
        selection3: Selection<T3>
    ): KConfigurableRootQuery<P, Tuple3<T1, T2, T3>> =
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
    ): KConfigurableRootQuery<P, Tuple4<T1, T2, T3, T4>> =
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
    ): KConfigurableRootQuery<P, Tuple5<T1, T2, T3, T4, T5>> =
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
    ): KConfigurableRootQuery<P, Tuple6<T1, T2, T3, T4, T5, T6>> =
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
    ): KConfigurableRootQuery<P, Tuple7<T1, T2, T3, T4, T5, T6, T7>> =
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
    ): KConfigurableRootQuery<P, Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> =
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
    ): KConfigurableRootQuery<P, Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> =
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

    override val subQueries: KSubQueries<P> =
        KSubQueriesImpl(javaQuery)

    override val wildSubQueries: KWildSubQueries<P> =
        KWildSubQueriesImpl(javaQuery)

    override fun hasVirtualPredicate(): Boolean =
        javaQuery.hasVirtualPredicate()

    override fun resolveVirtualPredicate(ctx: AstContext) {
        javaQuery.resolveVirtualPredicate(ctx)
    }

    internal class ForBaseTableImpl<B: KBaseTable>(
        javaTable: MutableRootQueryImpl<TableLike<*>>,
        override val table: B
    ): KMutableRootQueryImpl<B>(javaTable)

    internal class ForEntityImpl<E: Any>(
        javaQuery: MutableRootQueryImpl<TableLike<*>>
    ) : KMutableRootQueryImpl<KNonNullTable<E>>(javaQuery), KMutableRootQuery.ForEntity<E> {

        override val table: KNonNullTable<E> =
            KNonNullTableExImpl(javaQuery.getTable() as TableImplementor<E>)

        override fun where(specification: Specification<E>?) {
            if (specification != null) {
                val ks = specification as? KSpecification<E>
                    ?: throw IllegalArgumentException(
                        "The specification must be instance of \"${specification::class.qualifiedName}\""
                    )
                where(ks)
            }
        }

        override fun where(specification: KSpecification<E>?) {
            if (specification !== null) {
                val args = KSpecificationArgs<E>(
                    PredicateApplier(javaQuery)
                )
                specification.applyTo(args)
            }
        }
    }

    internal class ForAssociation<S, T>(
        javaQuery: MutableRootQueryImpl<TableLike<*>>
    ) : KMutableRootQueryImpl<KNonNullTable<Association<S, T>>>(javaQuery) {

        @Suppress("UNCHECKED_CAST")
        override val table: KNonNullTable<Association<S, T>> =
            KNonNullTableExImpl(javaQuery.tableLikeImplementor as TableImplementor<Association<S, T>>)
    }
}