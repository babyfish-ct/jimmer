package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.sql.ast.SqlTimeUnit
import org.babyfish.jimmer.sql.kt.ast.expression.impl.TimeDiffExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.TimePlusExpression
import java.time.temporal.Temporal

fun <T: Temporal> KNonNullExpression<T>.plus(
    value: KNonNullExpression<Long>,
    timeUnit: SqlTimeUnit
) : KNonNullExpression<T> =
    TimePlusExpression.NonNull(this, value, timeUnit)

fun <T: Temporal> KNonNullExpression<T>.plus(
    value: Long,
    timeUnit: SqlTimeUnit
) : KNonNullExpression<T> =
    TimePlusExpression.NonNull(this, value(value), timeUnit)

fun <T: Temporal> KNullableExpression<T>.plus(
    value: KNonNullExpression<Long>,
    timeUnit: SqlTimeUnit
) : KNullableExpression<T> =
    TimePlusExpression.Nullable(this, value, timeUnit)

fun <T: Temporal> KNullableExpression<T>.plus(
    value: Long,
    timeUnit: SqlTimeUnit
) : KNullableExpression<T> =
    TimePlusExpression.Nullable(this, value(value), timeUnit)

fun <T: Temporal> KNonNullExpression<T>.minus(
    value: KNonNullExpression<Long>,
    timeUnit: SqlTimeUnit
) : KNonNullExpression<T> =
    TimePlusExpression.NonNull(this, value.unaryMinus(), timeUnit)

fun <T: Temporal> KNonNullExpression<T>.minus(
    value: Long,
    timeUnit: SqlTimeUnit
) : KNonNullExpression<T> =
    TimePlusExpression.NonNull(this, value(-value), timeUnit)

fun <T: Temporal> KNullableExpression<T>.minus(
    value: KNonNullExpression<Long>,
    timeUnit: SqlTimeUnit
) : KNullableExpression<T> =
    TimePlusExpression.Nullable(this, value.unaryMinus(), timeUnit)

fun <T: Temporal> KNullableExpression<T>.minus(
    value: Long,
    timeUnit: SqlTimeUnit
) : KNullableExpression<T> =
    TimePlusExpression.Nullable(this, value(-value), timeUnit)

fun <T: Temporal> KNonNullExpression<T>.diff(
    other: KNonNullExpression<T>,
    timeUnit: SqlTimeUnit
) : KNonNullExpression<Float> =
    TimeDiffExpression.NonNull(this, other, timeUnit)

fun <T: Temporal> KNonNullExpression<T>.diff(
    other: T,
    timeUnit: SqlTimeUnit
) : KNonNullExpression<Float> =
    TimeDiffExpression.NonNull(this, value(other), timeUnit)

fun <T: Temporal> KNullableExpression<T>.diff(
    other: KExpression<T>,
    timeUnit: SqlTimeUnit
) : KNullableExpression<Float> =
    TimeDiffExpression.Nullable(this, other, timeUnit)

fun <T: Temporal> KNullableExpression<T>.diff(
    other: T,
    timeUnit: SqlTimeUnit
) : KNullableExpression<Float> =
    TimeDiffExpression.Nullable(this, value(other), timeUnit)