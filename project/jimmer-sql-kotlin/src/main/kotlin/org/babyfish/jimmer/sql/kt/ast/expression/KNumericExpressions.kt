package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.sql.kt.ast.expression.impl.BinaryOperatorExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NonNullUnaryMinisExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullableUnaryMinisExpression

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

operator fun <N: Number> KNonNullExpression<N>.unaryPlus() : KNonNullExpression<N> =
    this

operator fun <N: Number> KNullableExpression<N>.unaryPlus() : KNullableExpression<N> =
    this

operator fun <N: Number> KNonNullExpression<N>.unaryMinus() : KNonNullExpression<N> =
    if (this is NonNullUnaryMinisExpression<N>) {
        this.expression
    } else {
        NonNullUnaryMinisExpression(this)
    }

operator fun <N: Number> KNullableExpression<N>.unaryMinus() : KNullableExpression<N> =
    if (this is NullableUnaryMinisExpression<N>) {
        this.expression
    } else {
        NullableUnaryMinisExpression(this)
    }