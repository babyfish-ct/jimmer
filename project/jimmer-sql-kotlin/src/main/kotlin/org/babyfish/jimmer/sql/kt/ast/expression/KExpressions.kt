package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection
import org.babyfish.jimmer.sql.ast.query.NullOrderMode
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.ast.tuple.*
import org.babyfish.jimmer.sql.kt.ast.expression.impl.*
import org.babyfish.jimmer.sql.kt.ast.expression.impl.ConstantExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.LiteralExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullExpression
import org.babyfish.jimmer.sql.kt.ast.query.KMutableSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.KTypedSubQuery
import org.babyfish.jimmer.sql.kt.ast.table.KTable
import kotlin.reflect.KClass



fun <T: Any> KNullableExpression<T>.asNonNull(): KNonNullExpression<T> =
    if (this is NullableExpressionWrapper<*>) {
        (this as NullableExpressionWrapper<T>).target
    } else {
        NonNullExpressionWrapper(this)
    }

fun <T: Any> KNonNullExpression<T>.asNullable(): KNullableExpression<T> =
    if (this is NonNullExpressionWrapper<*>) {
        (this as NonNullExpressionWrapper<T>).target
    } else {
        NullableExpressionWrapper(this)
    }



fun and(vararg predicates: KNonNullExpression<Boolean>): KNonNullExpression<Boolean> =
    if (predicates.size == 1) {
        predicates[0]
    } else {
        AndPredicate(predicates.map { it.toKtPredicateImpl() })
    }

fun or(vararg predicates: KNonNullExpression<Boolean>): KNonNullExpression<Boolean> =
    if (predicates.size == 1) {
        predicates[0]
    } else {
        OrPredicate(predicates.map { it.toKtPredicateImpl() })
    }

fun KNonNullExpression<Boolean>.not(): KNonNullExpression<Boolean> =
    toKtPredicateImpl().not()



fun KExpression<*>.isNull(): KNonNullExpression<Boolean> =
    IsNullPredicate(this)

fun KExpression<*>.isNotNull(): KNonNullExpression<Boolean> =
    IsNotNullPredicate(this)



fun <T: Any> value(value: T): KNonNullExpression<T> =
    LiteralExpression(value)

fun <T: Any> nullValue(type: KClass<T>): KNullableExpression<T> =
    NullExpression(type.java)

fun <T: Number> constant(value: T): KNonNullExpression<T> =
    ConstantExpression(value)



fun <T: Any> sql(type: KClass<T>, sql: String, block: (SqlDSL.() -> Unit)? = null): KNonNullExpression<T> {
    val dsl = SqlDSL(sql)
    if (block !== null) {
        dsl.block()
    }
    return NonNullNativeExpression(type.java, dsl.parts())
}

fun <T: Any> sqlNullable(type: KClass<T>, sql: String, block: (SqlDSL.() -> Unit)? = null): KNullableExpression<T> {
    val dsl = SqlDSL(sql)
    if (block !== null) {
        dsl.block()
    }
    return NullableNativeExpression(type.java, dsl.parts())
}



infix fun <E: Any> KTable<E>.eq(right: KTable<E>): KNonNullExpression<Boolean> {
    val immutableType = (this as TableSelection).immutableType
    if (immutableType !== (right as TableSelection).immutableType) {
        throw IllegalArgumentException("Different table can not be compared")
    }
    val idPropName = immutableType.idProp.name
    val idExpr: KPropExpression<Any> = get(idPropName)
    val rightIdExpr: KPropExpression<Any> = right.get(idPropName)
    return ComparisonPredicate.Eq(idExpr, rightIdExpr)
}

infix fun <T: Any> KExpression<T>.eq(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Eq(this, right)

infix fun <T: Any> KExpression<T>.eq(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Eq(this, value(right))

infix fun <T: Any> KExpression<T>.ne(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Ne(this, right)

infix fun <T: Any> KExpression<T>.ne(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Ne(this, value(right))

infix fun <T: Comparable<*>> KExpression<T>.lt(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Lt(this, right)

infix fun <T: Comparable<*>> KExpression<T>.lt(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Lt(this, value(right))

infix fun <T: Comparable<*>> KExpression<T>.le(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Le(this, right)

infix fun <T: Comparable<*>> KExpression<T>.le(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Le(this, value(right))

infix fun <T: Comparable<*>> KExpression<T>.gt(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Gt(this, right)

infix fun <T: Comparable<*>> KExpression<T>.gt(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Gt(this, value(right))

infix fun <T: Comparable<*>> KExpression<T>.ge(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Ge(this, right)

infix fun <T: Comparable<*>> KExpression<T>.ge(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Ge(this, value(right))

fun <T: Comparable<*>> KExpression<T>.between(
    min: KNonNullExpression<T>,
    max: KNonNullExpression<T>
): KNonNullExpression<Boolean> =
    BetweenPredicate(false, this, min, max)

fun <T: Comparable<*>> KExpression<T>.between(
    min: T,
    max: T
): KNonNullExpression<Boolean> =
    BetweenPredicate(false, this, value(min), value(max))

fun <T: Comparable<*>> KExpression<T>.notBetween(
    min: KNonNullExpression<T>,
    max: KNonNullExpression<T>
): KNonNullExpression<Boolean> =
    BetweenPredicate(true, this, min, max)

fun <T: Comparable<*>> KExpression<T>.notBetween(
    min: T,
    max: T
): KNonNullExpression<Boolean> =
    BetweenPredicate(true, this, value(min), value(max))



operator fun <N: Number> KNonNullExpression<N>.plus(right: KNonNullExpression<N>): KNonNullExpression<N> =
    BinaryOperatorExpression.NonNullPlus(this, right)

operator fun <N: Number> KNonNullExpression<N>.plus(right: N): KNonNullExpression<N> =
    BinaryOperatorExpression.NonNullPlus(this, value(right))

operator fun <N: Number> KExpression<N>.plus(right: KExpression<N>): KNullableExpression<N> =
    BinaryOperatorExpression.NullablePlus(this, right)

operator fun <N: Number> KExpression<N>.plus(right: N): KNullableExpression<N> =
    BinaryOperatorExpression.NullablePlus(this, value(right))

operator fun <N: Number> KNonNullExpression<N>.minus(right: KNonNullExpression<N>): KNonNullExpression<N> =
    BinaryOperatorExpression.NonNullMinus(this, right)

operator fun <N: Number> KNonNullExpression<N>.minus(right: N): KNonNullExpression<N> =
    BinaryOperatorExpression.NonNullMinus(this, value(right))

operator fun <N: Number> KExpression<N>.minus(right: KExpression<N>): KNullableExpression<N> =
    BinaryOperatorExpression.NullableMinus(this, right)

operator fun <N: Number> KExpression<N>.minus(right: N): KNullableExpression<N> =
    BinaryOperatorExpression.NullableMinus(this, value(right))

operator fun <N: Number> KNonNullExpression<N>.times(right: KNonNullExpression<N>): KNonNullExpression<N> =
    BinaryOperatorExpression.NonNullTimes(this, right)

operator fun <N: Number> KNonNullExpression<N>.times(right: N): KNonNullExpression<N> =
    BinaryOperatorExpression.NonNullTimes(this, value(right))

operator fun <N: Number> KExpression<N>.times(right: KExpression<N>): KNullableExpression<N> =
    BinaryOperatorExpression.NullableTimes(this, right)

operator fun <N: Number> KExpression<N>.times(right: N): KNullableExpression<N> =
    BinaryOperatorExpression.NullableTimes(this, value(right))

operator fun <N: Number> KNonNullExpression<N>.div(right: KNonNullExpression<N>): KNonNullExpression<N> =
    BinaryOperatorExpression.NonNullDiv(this, right)

operator fun <N: Number> KNonNullExpression<N>.div(right: N): KNonNullExpression<N> =
    BinaryOperatorExpression.NonNullDiv(this, value(right))

operator fun <N: Number> KExpression<N>.div(right: KExpression<N>): KNullableExpression<N> =
    BinaryOperatorExpression.NullableDiv(this, right)

operator fun <N: Number> KExpression<N>.div(right: N): KNullableExpression<N> =
    BinaryOperatorExpression.NullableDiv(this, value(right))

operator fun <N: Number> KNonNullExpression<N>.rem(right: KNonNullExpression<N>): KNonNullExpression<N> =
    BinaryOperatorExpression.NonNullRem(this, right)

operator fun <N: Number> KNonNullExpression<N>.rem(right: N): KNonNullExpression<N> =
    BinaryOperatorExpression.NonNullRem(this, value(right))

operator fun <N: Number> KExpression<N>.rem(right: KExpression<N>): KNullableExpression<N> =
    BinaryOperatorExpression.NullableRem(this, right)

operator fun <N: Number> KExpression<N>.rem(right: N): KNullableExpression<N> =
    BinaryOperatorExpression.NullableRem(this, value(right))



fun count(expression: KExpression<*>, distinct: Boolean = false): KNonNullExpression<Long> =
    if (distinct) {
        AggregationExpression.CountDistinct(expression)
    } else {
        AggregationExpression.Count(expression)
    }

fun count(table: KTable<*>, distinct: Boolean = false): KNonNullExpression<Long> {
    val idProp = (table as TableSelection).immutableType.idProp
    val idExpr = table.get<Any, KPropExpression<Any>>(idProp.name)
    return count(idExpr, distinct)
}

fun <T: Comparable<*>> max(expression: KExpression<T>): KNullableExpression<T> =
    AggregationExpression.Max(expression)

fun <T: Comparable<*>> min(expression: KExpression<T>): KNullableExpression<T> =
    AggregationExpression.Min(expression)

fun <T: Number> sum(expression: KExpression<T>): KNullableExpression<T> =
    AggregationExpression.Sum(expression)

fun <T: Number> avg(expression: KExpression<T>): KNullableExpression<T> =
    AggregationExpression.Avg(expression)



fun <T: Any> case(value: KExpression<T>): SimpleCaseStarter<T> =
    SimpleCaseStarter(value)

fun case(): CaseStarter = CaseStarter



infix fun KExpression<String>.like(
    pattern: String
): KNonNullExpression<Boolean> =
    LikePredicate(this, false, pattern, LikeMode.ANYWHERE)

infix fun KExpression<String>.ilike(
    pattern: String
): KNonNullExpression<Boolean> =
    LikePredicate(this, true, pattern, LikeMode.ANYWHERE)

fun KExpression<String>.like(
    pattern: String,
    mode: LikeMode
): KNonNullExpression<Boolean> =
    LikePredicate(this, false, pattern, mode)

fun KExpression<String>.ilike(
    pattern: String,
    mode: LikeMode
): KNonNullExpression<Boolean> =
    LikePredicate(this, true, pattern, mode)



fun <T: Any> KNullableExpression<T>.coalesce(): NullableCoalesce<T> =
    NullableCoalesce(null, this)

fun <T: Any> KNullableExpression<T>.coalesce(
    defaultValue: KNonNullExpression<T>
): KNonNullExpression<T> =
    coalesce().or(defaultValue).end()

fun <T: Any> KNullableExpression<T>.coalesce(
    defaultValue: T
): KNonNullExpression<T> =
    coalesce().or(value(defaultValue)).end()



fun concat(vararg expressions: KNonNullExpression<String>): KNonNullExpression<String> =
    ConcatExpression.NonNull(expressions.toList())

fun concat(vararg expressions: KExpression<String>): KNullableExpression<String> =
    ConcatExpression.Nullable(expressions.toList())



infix fun <T: Any> KExpression<T>.valueIn(
    values: Collection<T>
): KNonNullExpression<Boolean> =
    InCollectionPredicate(false, this, values)

infix fun <T: Any> KExpression<T>.valueNotIn(
    values: Collection<T>
): KNonNullExpression<Boolean> =
    InCollectionPredicate(true, this, values)

infix fun <T: Any> KExpression<T>.valueIn(
    subQuery: KTypedSubQuery<T>
): KNonNullExpression<Boolean> =
    InSubQueryPredicate(false, this, subQuery)

infix fun <T: Any> KExpression<T>.valueNotIn(
    subQuery: KTypedSubQuery<T>
): KNonNullExpression<Boolean> =
    InSubQueryPredicate(true, this, subQuery)



fun <T1, T2> tuple(
    selection1: Selection<T1>,
    selection2: Selection<T2>
): KNonNullExpression<Tuple2<T1, T2>> =
    Tuple2Expression(
        selection1,
        selection2
    )

fun <T1, T2, T3> tuple(
    selection1: Selection<T1>,
    selection2: Selection<T2>,
    selection3: Selection<T3>
): KNonNullExpression<Tuple3<T1, T2, T3>> =
    Tuple3Expression(
        selection1,
        selection2,
        selection3
    )

fun <T1, T2, T3, T4> tuple(
    selection1: Selection<T1>,
    selection2: Selection<T2>,
    selection3: Selection<T3>,
    selection4: Selection<T4>
): KNonNullExpression<Tuple4<T1, T2, T3, T4>> =
    Tuple4Expression(
        selection1,
        selection2,
        selection3,
        selection4
    )

fun <T1, T2, T3, T4, T5> tuple(
    selection1: Selection<T1>,
    selection2: Selection<T2>,
    selection3: Selection<T3>,
    selection4: Selection<T4>,
    selection5: Selection<T5>
): KNonNullExpression<Tuple5<T1, T2, T3, T4, T5>> =
    Tuple5Expression(
        selection1,
        selection2,
        selection3,
        selection4,
        selection5
    )

fun <T1, T2, T3, T4, T5, T6> tuple(
    selection1: Selection<T1>,
    selection2: Selection<T2>,
    selection3: Selection<T3>,
    selection4: Selection<T4>,
    selection5: Selection<T5>,
    selection6: Selection<T6>
): KNonNullExpression<Tuple6<T1, T2, T3, T4, T5, T6>> =
    Tuple6Expression(
        selection1,
        selection2,
        selection3,
        selection4,
        selection5,
        selection6
    )

fun <T1, T2, T3, T4, T5, T6, T7> tuple(
    selection1: Selection<T1>,
    selection2: Selection<T2>,
    selection3: Selection<T3>,
    selection4: Selection<T4>,
    selection5: Selection<T5>,
    selection6: Selection<T6>,
    selection7: Selection<T7>
): KNonNullExpression<Tuple7<T1, T2, T3, T4, T5, T6, T7>> =
    Tuple7Expression(
        selection1,
        selection2,
        selection3,
        selection4,
        selection5,
        selection6,
        selection7
    )

fun <T1, T2, T3, T4, T5, T6, T7, T8> tuple(
    selection1: Selection<T1>,
    selection2: Selection<T2>,
    selection3: Selection<T3>,
    selection4: Selection<T4>,
    selection5: Selection<T5>,
    selection6: Selection<T6>,
    selection7: Selection<T7>,
    selection8: Selection<T8>
): KNonNullExpression<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> =
    Tuple8Expression(
        selection1,
        selection2,
        selection3,
        selection4,
        selection5,
        selection6,
        selection7,
        selection8
    )

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> tuple(
    selection1: Selection<T1>,
    selection2: Selection<T2>,
    selection3: Selection<T3>,
    selection4: Selection<T4>,
    selection5: Selection<T5>,
    selection6: Selection<T6>,
    selection7: Selection<T7>,
    selection8: Selection<T8>,
    selection9: Selection<T9>
): KNonNullExpression<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> =
    Tuple9Expression(
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



fun <T: Any> all(subQuery: KTypedSubQuery.NonNull<T>): KNonNullExpression<T> =
    SubQueryFunExpression.AllNonNull(subQuery)

fun <T: Any> all(subQuery: KTypedSubQuery.Nullable<T>): KNullableExpression<T> =
    SubQueryFunExpression.AllNullable(subQuery)

fun <T: Any> any(subQuery: KTypedSubQuery.NonNull<T>): KNonNullExpression<T> =
    SubQueryFunExpression.AnyNonNull(subQuery)

fun <T: Any> any(subQuery: KTypedSubQuery.Nullable<T>): KNullableExpression<T> =
    SubQueryFunExpression.AnyNullable(subQuery)



fun exists(subQuery: KTypedSubQuery<*>): KNonNullExpression<Boolean> =
    ExistsPredicate(false, subQuery)

fun notExists(subQuery: KTypedSubQuery<*>): KNonNullExpression<Boolean> =
    ExistsPredicate(true, subQuery)

fun exists(subQuery: KMutableSubQuery<*, *>): KNonNullExpression<Boolean> =
    ExistsPredicate(false, subQuery.select(constant(1)))

fun notExists(subQuery: KMutableSubQuery<*, *>): KNonNullExpression<Boolean> =
    ExistsPredicate(true, subQuery.select(constant(1)))



fun KExpression<*>.asc() =
    Order(this as Expression<*>, OrderMode.ASC, NullOrderMode.UNSPECIFIED)

fun KExpression<*>.desc() =
    Order(this as Expression<*>, OrderMode.DESC, NullOrderMode.UNSPECIFIED)
