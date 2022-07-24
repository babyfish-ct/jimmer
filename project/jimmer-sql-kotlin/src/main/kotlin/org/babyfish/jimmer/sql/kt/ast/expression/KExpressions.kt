package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.sql.ast.impl.table.TableSelection
import org.babyfish.jimmer.sql.kt.ast.expression.impl.*
import org.babyfish.jimmer.sql.kt.ast.expression.impl.ConstantExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.LiteralExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullExpression
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



fun KExpression<*>.isNull(): KNonNullExpression<Boolean> =
    TODO()

fun KExpression<*>.isNotNull(): KNonNullExpression<Boolean> =
    TODO()



fun <T: Any> value(value: T): KNonNullExpression<T> =
    LiteralExpression(value)

fun <T: Any> nullValue(type: KClass<T>): KNullableExpression<T> =
    NullExpression(type.java)

fun <T: Number> constant(value: T): KNonNullExpression<T> =
    ConstantExpression(value)



fun <T: Any> sql(type: KClass<T>, sql: String, block: SqlDSL.() -> Unit): KNonNullExpression<T> {
    val dsl = SqlDSL(sql)
    dsl.block()
    return NonNullNativeExpression(type.java, dsl.parts())
}

fun <T: Any> sqlNullable(type: KClass<T>, sql: String, block: SqlDSL.() -> Unit): KNullableExpression<T> {
    val dsl = SqlDSL(sql)
    dsl.block()
    return NullableNativeExpression(type.java, dsl.parts())
}



infix fun <T: Comparable<T>> KExpression<T>.eq(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Eq(this, right)

infix fun <T: Comparable<T>> KExpression<T>.eq(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Eq(this, value(right))

infix fun <T: Comparable<T>> KExpression<T>.ne(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Ne(this, right)

infix fun <T: Comparable<T>> KExpression<T>.ne(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Ne(this, value(right))

infix fun <T: Comparable<T>> KExpression<T>.lt(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Lt(this, right)

infix fun <T: Comparable<T>> KExpression<T>.lt(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Lt(this, value(right))

infix fun <T: Comparable<T>> KExpression<T>.le(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Le(this, right)

infix fun <T: Comparable<T>> KExpression<T>.le(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Le(this, value(right))

infix fun <T: Comparable<T>> KExpression<T>.gt(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Gt(this, right)

infix fun <T: Comparable<T>> KExpression<T>.gt(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Gt(this, value(right))

infix fun <T: Comparable<T>> KExpression<T>.ge(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Ge(this, right)

infix fun <T: Comparable<T>> KExpression<T>.ge(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Ge(this, value(right))



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
    val idProp = (table as TableSelection<*>).immutableType.idProp
    val idExpr = table.get<Any, KPropExpression<Any>>(idProp.name)
    return count(idExpr, distinct)
}