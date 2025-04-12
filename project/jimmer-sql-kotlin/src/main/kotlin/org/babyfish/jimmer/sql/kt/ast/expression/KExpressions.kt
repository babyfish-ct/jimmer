package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.query.NullOrderMode
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.ast.tuple.*
import org.babyfish.jimmer.sql.kt.ast.expression.impl.*
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NumberConstantExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.LiteralExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullExpression
import org.babyfish.jimmer.sql.kt.ast.query.*
import org.babyfish.jimmer.sql.kt.ast.query.impl.KConfigurableSubQueryImpl
import org.babyfish.jimmer.sql.kt.ast.table.KNullableTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KTable
import java.math.BigDecimal
import kotlin.reflect.KClass

fun <T: Any> KNullablePropExpression<T>.asNonNull(): KNonNullPropExpression<T> =
    NonNullPropExpressionImpl((this as NullablePropExpressionImpl<T>).javaPropExpression)

fun <T: Any> KNullableExpression<T>.asNonNull(): KNonNullExpression<T> =
    when (this) {
        is NullableExpressionWrapper<*> ->
            (this as NullableExpressionWrapper<T>).target
        is KNullablePropExpression<*> ->
            NonNullPropExpressionImpl((this as NullablePropExpressionImpl<T>).javaPropExpression)
        else ->
            NonNullExpressionWrapper(this)
    }

fun <T: Any> KNonNullPropExpression<T>.asNullable(): KNullablePropExpression<T> =
    NullablePropExpressionImpl((this as NonNullPropExpressionImpl<T>).javaPropExpression)

fun <T: Any> KNonNullExpression<T>.asNullable(): KNullableExpression<T> =
    when (this) {
        is NonNullExpressionWrapper<*> ->
            (this as NonNullExpressionWrapper<T>).target
        is KNonNullPropExpression<*> ->
            NullablePropExpressionImpl((this as NonNullPropExpressionImpl<T>).javaPropExpression)
        else ->
            NullableExpressionWrapper(this)
    }

@Suppress("UNCHECKED_CAST")
fun <T: Any> value(value: T): KNonNullExpression<T> =
    when (value) {
        is KNonNullExpression<*> -> value as KNonNullExpression<T>
        is KNullableTableEx<*> -> (value as KNullableExpression<T>).asNonNull()
        else -> LiteralExpression(value)
    }

fun <T: Any> nullValue(type: KClass<T>): KNullableExpression<T> =
    NullExpression(type.java)

inline fun <reified T: Any> nullValue() = nullValue(T::class)

fun <T: Number> constant(value: T): KNonNullExpression<T> =
    NumberConstantExpression(value)

fun <T: Enum<T>> constant(value: T): KNonNullExpression<T> =
    EnumConstantExpression(value)

fun constant(value: String): KNonNullExpression<String> =
    StringConstantExpression(value)

@Suppress("UNCHECKED_CAST")
fun <T: Any> sql(type: KClass<T>, sql: String, block: NativeDsl.() -> Unit): KNonNullExpression<T> {
    val dsl = NativeDsl(sql)
    dsl.block()
    if (type == Boolean::class) {
        return NativePredicate(dsl.parts()) as KNonNullExpression<T>
    }
    return NonNullNativeExpression(type.java, dsl.parts())
}

fun <T: Any> sql(
    type: KClass<T>,
    sql: String,
    vararg  expressions: KExpression<*>
): KNonNullExpression<T> =
    sql(type, sql) {
        for (expression in expressions) {
            expression(expression)
        }
    }

inline fun <reified T : Any> sql(
    sql: String,
    noinline block: NativeDsl.() -> Unit
): KNonNullExpression<T> = sql(T::class, sql, block)

inline fun <reified T : Any> sql(
    sql: String,
    vararg expressions: KExpression<*>
): KNonNullExpression<T> = sql(T::class, sql, *expressions)

fun <T: Any> sqlNullable(
    type: KClass<T>,
    sql: String,
    block: NativeDsl.() -> Unit
): KNullableExpression<T> {
    val dsl = NativeDsl(sql)
    dsl.block()
    return NullableNativeExpression(type.java, dsl.parts())
}

fun <T: Any> sqlNullable(
    type: KClass<T>,
    sql: String,
    vararg expressions: KExpression<*>
): KNullableExpression<T> = sqlNullable(type, sql) {
    for (expression in expressions) {
        expression(expression)
    }
}

inline fun <reified T : Any> sqlNullable(
    sql: String,
    noinline block: NativeDsl.() -> Unit
): KNullableExpression<T> =
    sqlNullable(T::class, sql, block)

inline fun <reified T: Any> sqlNullable(
    sql: String,
    vararg expressions: KExpression<*>
): KNullableExpression<T> =
    sqlNullable(T::class, sql, *expressions)

fun rowCount(): KNonNullExpression<Long> {
    return ROW_COUNT
}

private val ROW_COUNT = count(constant(1))

fun count(expression: KExpression<*>, distinct: Boolean = false): KNonNullExpression<Long> =
    if (distinct) {
        AggregationExpression.CountDistinct(expression)
    } else {
        AggregationExpression.Count(expression)
    }


fun count(table: KTable<*>, distinct: Boolean = false): KNonNullExpression<Long> {
    val idExpr = table.getId<Any>()
    return count(idExpr, distinct)
}

fun <T: Comparable<*>> max(expression: KExpression<T>): KNullableExpression<T> =
    AggregationExpression.Max(expression)

fun <T: Comparable<*>> min(expression: KExpression<T>): KNullableExpression<T> =
    AggregationExpression.Min(expression)

fun <T: Number> sum(expression: KExpression<T>): KNullableExpression<T> =
    AggregationExpression.Sum(expression)

fun sumAsLong(expression: KExpression<Int>): KNullableExpression<Long> =
    AggregationExpression.SumAsLong(expression)

fun <T: Number> avg(expression: KExpression<T>): KNullableExpression<Double> =
    AggregationExpression.Avg(expression)

fun <T: Number> avgAsDecimal(expression: KExpression<T>): KNullableExpression<BigDecimal> =
    AggregationExpression.AvgAsDecimal(expression)

fun <T: Any> case(value: KExpression<T>): SimpleCaseStarter<T> =
    SimpleCaseStarter(value)

fun case(): CaseStarter = CaseStarter



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
    if (subQuery is KConfigurableSubQueryImpl<*>) {
        subQuery.javaSubQuery.exists().toKotlinPredicate()
    } else {
        ExistsPredicate(false, subQuery)
    }

fun notExists(subQuery: KTypedSubQuery<*>): KNonNullExpression<Boolean> =
    if (subQuery is KConfigurableSubQueryImpl<*>) {
        subQuery.javaSubQuery.notExists().toKotlinPredicate()
    } else {
        ExistsPredicate(true, subQuery)
    }

fun exists(subQuery: KMutableSubQuery<*, *>): KNonNullExpression<Boolean> =
    ExistsPredicate(false, subQuery.select(constant(1)))

fun notExists(subQuery: KMutableSubQuery<*, *>): KNonNullExpression<Boolean> =
    ExistsPredicate(true, subQuery.select(constant(1)))



fun KExpression<*>.asc() =
    Order(this as Expression<*>, OrderMode.ASC, NullOrderMode.UNSPECIFIED)

fun KExpression<*>.desc() =
    Order(this as Expression<*>, OrderMode.DESC, NullOrderMode.UNSPECIFIED)
