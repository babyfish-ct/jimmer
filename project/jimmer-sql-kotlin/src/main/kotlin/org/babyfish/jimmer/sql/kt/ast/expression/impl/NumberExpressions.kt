package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

operator fun <T: Number> KExpression<T>.plus(right: KExpression<T>) : KNullableExpression<T> {
    TODO()
}

operator fun <T: Number> KNonNullExpression<T>.plus(right: KNonNullExpression<T>): KNonNullExpression<T> {
    TODO()
}

operator fun <T: Number> KExpression<T>.plus(right: T): KNullableExpression<T> {
    TODO()
}

operator fun <T: Number> KNonNullExpression<T>.plus(right: T): KNonNullExpression<T> {
    TODO()
}

operator fun <T: Number> KExpression<T>.minus(right: KExpression<T>) : KNullableExpression<T> {
    TODO()
}

operator fun <T: Number> KNonNullExpression<T>.minus(right: KNonNullExpression<T>): KNonNullExpression<T> {
    TODO()
}

operator fun <T: Number> KExpression<T>.minus(right: T): KNullableExpression<T> {
    TODO()
}

operator fun <T: Number> KNonNullExpression<T>.minus(right: T): KNonNullExpression<T> {
    TODO()
}

operator fun <T: Number> KExpression<T>.times(right: KExpression<T>) : KNullableExpression<T> {
    TODO()
}

operator fun <T: Number> KNonNullExpression<T>.times(right: KNonNullExpression<T>): KNonNullExpression<T> {
    TODO()
}

operator fun <T: Number> KExpression<T>.times(right: T): KNullableExpression<T> {
    TODO()
}

operator fun <T: Number> KNonNullExpression<T>.times(right: T): KNonNullExpression<T> {
    TODO()
}

operator fun <T: Number> KExpression<T>.div(right: KExpression<T>) : KNullableExpression<T> {
    TODO()
}

operator fun <T: Number> KNonNullExpression<T>.div(right: KNonNullExpression<T>): KNonNullExpression<T> {
    TODO()
}

operator fun <T: Number> KExpression<T>.div(right: T): KNullableExpression<T> {
    TODO()
}

operator fun <T: Number> KNonNullExpression<T>.div(right: T): KNonNullExpression<T> {
    TODO()
}

operator fun <T: Number> KExpression<T>.rem(right: KExpression<T>) : KNullableExpression<T> {
    TODO()
}

operator fun <T: Number> KNonNullExpression<T>.rem(right: KNonNullExpression<T>): KNonNullExpression<T> {
    TODO()
}

operator fun <T: Number> KExpression<T>.rem(right: T): KNullableExpression<T> {
    TODO()
}

operator fun <T: Number> KNonNullExpression<T>.rem(right: T): KNonNullExpression<T> {
    TODO()
}
