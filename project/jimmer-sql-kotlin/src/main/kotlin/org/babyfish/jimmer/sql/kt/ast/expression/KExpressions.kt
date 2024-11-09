package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.PredicateImplementor
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection
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
import org.babyfish.jimmer.sql.kt.ast.table.impl.KTableImplementor
import java.math.BigDecimal
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



fun and(vararg predicates: KNonNullExpression<Boolean>?): KNonNullExpression<Boolean>? =
    predicates.filterNotNull().let {
        when (it.size) {
            0 -> null
            1 -> it[0]
            else -> AndPredicate(it)
        }
    }

fun or(vararg predicates: KNonNullExpression<Boolean>?): KNonNullExpression<Boolean>? =
    predicates.filterNotNull().let {
        when (it.size) {
            0 -> null
            1 -> it[0]
            else -> OrPredicate(it)
        }
    }


fun KNonNullExpression<Boolean>.not(): KNonNullExpression<Boolean> =
    this.toJavaPredicate().not().toKotlinPredicate()

fun KExpression<*>.isNull(): KNonNullExpression<Boolean> =
    IsNullPredicate(this)

fun KExpression<*>.isNotNull(): KNonNullExpression<Boolean> =
    IsNotNullPredicate(this)



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
fun <T: Any> sql(type: KClass<T>, sql: String, block: (SqlDSL.() -> Unit)? = null): KNonNullExpression<T> {
    val dsl = SqlDSL(sql)
    if (block !== null) {
        dsl.block()
    }
    if (type == Boolean::class) {
        return NativePredicate(dsl.parts()) as KNonNullExpression<T>
    }
    return NonNullNativeExpression(type.java, dsl.parts())
}

inline fun <reified T : Any> sql(
    sql: String,
    noinline block: (SqlDSL.() -> Unit)? = null
): KNonNullExpression<T> = sql(T::class, sql, block)

fun <T: Any> sqlNullable(type: KClass<T>, sql: String, block: (SqlDSL.() -> Unit)? = null): KNullableExpression<T> {
    val dsl = SqlDSL(sql)
    if (block !== null) {
        dsl.block()
    }
    return NullableNativeExpression(type.java, dsl.parts())
}

inline fun <reified T : Any> sqlNullable(
    sql: String,
    noinline block: (SqlDSL.() -> Unit)? = null
): KNullableExpression<T> = sqlNullable(T::class, sql, block)


/**
 * Shortcut for `this.id eq right `
 */
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

/**
 * QBE
 */
infix fun <E: Any> KTable<E>.eq(right: E): KNonNullExpression<Boolean>? =
    eq(example(right))

/**
 * QBE
 */
infix fun <E: Any> KTable<E>.eq(right: View<E>): KNonNullExpression<Boolean>? =
    eq(viewExample(right))

/**
 * QBE
 */
infix fun <E: Any> KTable<E>.eq(right: KExample<E>): KNonNullExpression<Boolean>? =
    right.toPredicate((this as KTableImplementor<*>).javaTable)?.let {
        JavaToKotlinPredicateWrapper(it as PredicateImplementor)
    }

infix fun <T: Any> KExpression<T>.eq(right: KExpression<T>): KNonNullExpression<Boolean> =
    if (right is NullExpression<*>) {
        IsNullPredicate(this)
    } else if (this is NullExpression<*>) {
        IsNullPredicate(right)
    } else {
        ComparisonPredicate.Eq(this, right)
    }

infix fun <T: Any> KExpression<T>.eq(right: T?): KNonNullExpression<Boolean> =
    if (right === null) {
        isNull()
    } else {
        ComparisonPredicate.Eq(this, value(right))
    }

infix fun <T: Any> KExpression<T>.`eq?`(right: T?): KNonNullExpression<Boolean>? =
    right?.takeIf { it != "" }?.let {
        ComparisonPredicate.Eq(this, value(it))
    }

infix fun <T: Any> KExpression<T>.ne(right: KExpression<T>): KNonNullExpression<Boolean> =
    if (right is NullExpression<*>) {
        IsNotNullPredicate(this)
    } else if (this is NullExpression<*>) {
        IsNotNullPredicate(right)
    } else {
        ComparisonPredicate.Ne(this, right)
    }

infix fun <T: Any> KExpression<T>.ne(right: T?): KNonNullExpression<Boolean> =
    if (right === null) {
        isNotNull()
    } else {
        ComparisonPredicate.Ne(this, value(right))
    }

infix fun <T: Any> KExpression<T>.`ne?`(right: T?): KNonNullExpression<Boolean>? =
    right?.takeIf { it != "" }?.let {
        ComparisonPredicate.Ne(this, value(it))
    }

infix fun <T: Comparable<*>> KExpression<T>.lt(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Lt(this, right)

infix fun <T: Comparable<*>> KExpression<T>.lt(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Lt(this, value(right))

infix fun <T: Comparable<*>> KExpression<T>.`lt?`(right: T?): KNonNullExpression<Boolean>? =
    right?.takeIf { it != "" }?.let {
        ComparisonPredicate.Lt(this, value(it))
    }

infix fun <T: Comparable<*>> KExpression<T>.le(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Le(this, right)

infix fun <T: Comparable<*>> KExpression<T>.le(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Le(this, value(right))

infix fun <T: Comparable<*>> KExpression<T>.`le?`(right: T?): KNonNullExpression<Boolean>? =
    right?.takeIf { it != "" }?.let {
        ComparisonPredicate.Le(this, value(it))
    }

infix fun <T: Comparable<*>> KExpression<T>.gt(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Gt(this, right)

infix fun <T: Comparable<*>> KExpression<T>.gt(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Gt(this, value(right))

infix fun <T: Comparable<*>> KExpression<T>.`gt?`(right: T?): KNonNullExpression<Boolean>? =
    right?.takeIf { it != "" }?.let {
        ComparisonPredicate.Gt(this, value(it))
    }

infix fun <T: Comparable<*>> KExpression<T>.ge(right: KExpression<T>): KNonNullExpression<Boolean> =
    ComparisonPredicate.Ge(this, right)

infix fun <T: Comparable<*>> KExpression<T>.ge(right: T): KNonNullExpression<Boolean> =
    ComparisonPredicate.Ge(this, value(right))

infix fun <T: Comparable<*>> KExpression<T>.`ge?`(right: T?): KNonNullExpression<Boolean>? =
    right?.takeIf { it != "" }?.let {
        ComparisonPredicate.Ge(this, value(it))
    }

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

fun <T: Comparable<*>> KExpression<T>.`between?`(
    min: T?,
    max: T?
): KNonNullExpression<Boolean>? {
    val finalMin = min?.takeIf { it != "" }
    val finalMax = max?.takeIf { it != "" }
    return when {
        finalMin === null && finalMax === null -> null
        finalMin === null -> ComparisonPredicate.Le(this, value(finalMax!!))
        finalMax === null -> ComparisonPredicate.Ge(this, value(finalMin))
        else -> BetweenPredicate(false, this, value(finalMin), value(finalMax))
    }
}

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

fun <T: Comparable<*>> KExpression<T>.`notBetween?`(
    min: T?,
    max: T?
): KNonNullExpression<Boolean>? {
    val finalMin = min?.takeIf { it != "" }
    val finalMax = max?.takeIf { it != "" }
    return when {
        finalMin === null && finalMax === null -> null
        finalMin === null -> ComparisonPredicate.Gt(this, value(finalMax!!))
        finalMax === null -> ComparisonPredicate.Lt(this, value(finalMin))
        else -> BetweenPredicate(true, this, value(finalMin), value(finalMax))
    }
}



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

object UnsignedLong {
    operator fun KNonNullExpression<ULong>.plus(right: KNonNullExpression<ULong>): KNonNullExpression<ULong> =
        BinaryOperatorExpression.NonNullPlus(this, right)

    operator fun KNonNullExpression<ULong>.plus(right: ULong): KNonNullExpression<ULong> =
        BinaryOperatorExpression.NonNullPlus(this, value(right))

    operator fun KExpression<ULong>.plus(right: KExpression<ULong>): KNullableExpression<ULong> =
        BinaryOperatorExpression.NullablePlus(this, right)

    operator fun KExpression<ULong>.plus(right: ULong): KNullableExpression<ULong> =
        BinaryOperatorExpression.NullablePlus(this, value(right))

    operator fun KNonNullExpression<ULong>.minus(right: KNonNullExpression<ULong>): KNonNullExpression<ULong> =
        BinaryOperatorExpression.NonNullMinus(this, right)

    operator fun KNonNullExpression<ULong>.minus(right: ULong): KNonNullExpression<ULong> =
        BinaryOperatorExpression.NonNullMinus(this, value(right))

    operator fun KExpression<ULong>.minus(right: KExpression<ULong>): KNullableExpression<ULong> =
        BinaryOperatorExpression.NullableMinus(this, right)

    operator fun KExpression<ULong>.minus(right: ULong): KNullableExpression<ULong> =
        BinaryOperatorExpression.NullableMinus(this, value(right))

    operator fun KNonNullExpression<ULong>.times(right: KNonNullExpression<ULong>): KNonNullExpression<ULong> =
        BinaryOperatorExpression.NonNullTimes(this, right)

    operator fun KNonNullExpression<ULong>.times(right: ULong): KNonNullExpression<ULong> =
        BinaryOperatorExpression.NonNullTimes(this, value(right))

    operator fun KExpression<ULong>.times(right: KExpression<ULong>): KNullableExpression<ULong> =
        BinaryOperatorExpression.NullableTimes(this, right)

    operator fun KExpression<ULong>.times(right: ULong): KNullableExpression<ULong> =
        BinaryOperatorExpression.NullableTimes(this, value(right))

    operator fun KNonNullExpression<ULong>.div(right: KNonNullExpression<ULong>): KNonNullExpression<ULong> =
        BinaryOperatorExpression.NonNullDiv(this, right)

    operator fun KNonNullExpression<ULong>.div(right: ULong): KNonNullExpression<ULong> =
        BinaryOperatorExpression.NonNullDiv(this, value(right))

    operator fun KExpression<ULong>.div(right: KExpression<ULong>): KNullableExpression<ULong> =
        BinaryOperatorExpression.NullableDiv(this, right)

    operator fun KExpression<ULong>.div(right: ULong): KNullableExpression<ULong> =
        BinaryOperatorExpression.NullableDiv(this, value(right))

    operator fun KNonNullExpression<ULong>.rem(right: KNonNullExpression<ULong>): KNonNullExpression<ULong> =
        BinaryOperatorExpression.NonNullRem(this, right)

    operator fun KNonNullExpression<ULong>.rem(right: ULong): KNonNullExpression<ULong> =
        BinaryOperatorExpression.NonNullRem(this, value(right))

    operator fun KExpression<ULong>.rem(right: KExpression<ULong>): KNullableExpression<ULong> =
        BinaryOperatorExpression.NullableRem(this, right)

    operator fun KExpression<ULong>.rem(right: ULong): KNullableExpression<ULong> =
        BinaryOperatorExpression.NullableRem(this, value(right))
}

object UnsignedInt {
    operator fun KNonNullExpression<UInt>.plus(right: KNonNullExpression<UInt>): KNonNullExpression<UInt> =
        BinaryOperatorExpression.NonNullPlus(this, right)

    operator fun KNonNullExpression<UInt>.plus(right: UInt): KNonNullExpression<UInt> =
        BinaryOperatorExpression.NonNullPlus(this, value(right))

    operator fun KExpression<UInt>.plus(right: KExpression<UInt>): KNullableExpression<UInt> =
        BinaryOperatorExpression.NullablePlus(this, right)

    operator fun KExpression<UInt>.plus(right: UInt): KNullableExpression<UInt> =
        BinaryOperatorExpression.NullablePlus(this, value(right))

    operator fun KNonNullExpression<UInt>.minus(right: KNonNullExpression<UInt>): KNonNullExpression<UInt> =
        BinaryOperatorExpression.NonNullMinus(this, right)

    operator fun KNonNullExpression<UInt>.minus(right: UInt): KNonNullExpression<UInt> =
        BinaryOperatorExpression.NonNullMinus(this, value(right))

    operator fun KExpression<UInt>.minus(right: KExpression<UInt>): KNullableExpression<UInt> =
        BinaryOperatorExpression.NullableMinus(this, right)

    operator fun KExpression<UInt>.minus(right: UInt): KNullableExpression<UInt> =
        BinaryOperatorExpression.NullableMinus(this, value(right))

    operator fun KNonNullExpression<UInt>.times(right: KNonNullExpression<UInt>): KNonNullExpression<UInt> =
        BinaryOperatorExpression.NonNullTimes(this, right)

    operator fun KNonNullExpression<UInt>.times(right: UInt): KNonNullExpression<UInt> =
        BinaryOperatorExpression.NonNullTimes(this, value(right))

    operator fun KExpression<UInt>.times(right: KExpression<UInt>): KNullableExpression<UInt> =
        BinaryOperatorExpression.NullableTimes(this, right)

    operator fun KExpression<UInt>.times(right: UInt): KNullableExpression<UInt> =
        BinaryOperatorExpression.NullableTimes(this, value(right))

    operator fun KNonNullExpression<UInt>.div(right: KNonNullExpression<UInt>): KNonNullExpression<UInt> =
        BinaryOperatorExpression.NonNullDiv(this, right)

    operator fun KNonNullExpression<UInt>.div(right: UInt): KNonNullExpression<UInt> =
        BinaryOperatorExpression.NonNullDiv(this, value(right))

    operator fun KExpression<UInt>.div(right: KExpression<UInt>): KNullableExpression<UInt> =
        BinaryOperatorExpression.NullableDiv(this, right)

    operator fun KExpression<UInt>.div(right: UInt): KNullableExpression<UInt> =
        BinaryOperatorExpression.NullableDiv(this, value(right))

    operator fun KNonNullExpression<UInt>.rem(right: KNonNullExpression<UInt>): KNonNullExpression<UInt> =
        BinaryOperatorExpression.NonNullRem(this, right)

    operator fun KNonNullExpression<UInt>.rem(right: UInt): KNonNullExpression<UInt> =
        BinaryOperatorExpression.NonNullRem(this, value(right))

    operator fun KExpression<UInt>.rem(right: KExpression<UInt>): KNullableExpression<UInt> =
        BinaryOperatorExpression.NullableRem(this, right)

    operator fun KExpression<UInt>.rem(right: UInt): KNullableExpression<UInt> =
        BinaryOperatorExpression.NullableRem(this, value(right))

}

object UnsignedShort {
    operator fun KNonNullExpression<UShort>.plus(right: KNonNullExpression<UShort>): KNonNullExpression<UShort> =
        BinaryOperatorExpression.NonNullPlus(this, right)

    operator fun KNonNullExpression<UShort>.plus(right: UShort): KNonNullExpression<UShort> =
        BinaryOperatorExpression.NonNullPlus(this, value(right))

    operator fun KExpression<UShort>.plus(right: KExpression<UShort>): KNullableExpression<UShort> =
        BinaryOperatorExpression.NullablePlus(this, right)

    operator fun KExpression<UShort>.plus(right: UShort): KNullableExpression<UShort> =
        BinaryOperatorExpression.NullablePlus(this, value(right))

    operator fun KNonNullExpression<UShort>.minus(right: KNonNullExpression<UShort>): KNonNullExpression<UShort> =
        BinaryOperatorExpression.NonNullMinus(this, right)

    operator fun KNonNullExpression<UShort>.minus(right: UShort): KNonNullExpression<UShort> =
        BinaryOperatorExpression.NonNullMinus(this, value(right))

    operator fun KExpression<UShort>.minus(right: KExpression<UShort>): KNullableExpression<UShort> =
        BinaryOperatorExpression.NullableMinus(this, right)

    operator fun KExpression<UShort>.minus(right: UShort): KNullableExpression<UShort> =
        BinaryOperatorExpression.NullableMinus(this, value(right))

    operator fun KNonNullExpression<UShort>.times(right: KNonNullExpression<UShort>): KNonNullExpression<UShort> =
        BinaryOperatorExpression.NonNullTimes(this, right)

    operator fun KNonNullExpression<UShort>.times(right: UShort): KNonNullExpression<UShort> =
        BinaryOperatorExpression.NonNullTimes(this, value(right))

    operator fun KExpression<UShort>.times(right: KExpression<UShort>): KNullableExpression<UShort> =
        BinaryOperatorExpression.NullableTimes(this, right)

    operator fun KExpression<UShort>.times(right: UShort): KNullableExpression<UShort> =
        BinaryOperatorExpression.NullableTimes(this, value(right))

    operator fun KNonNullExpression<UShort>.div(right: KNonNullExpression<UShort>): KNonNullExpression<UShort> =
        BinaryOperatorExpression.NonNullDiv(this, right)

    operator fun KNonNullExpression<UShort>.div(right: UShort): KNonNullExpression<UShort> =
        BinaryOperatorExpression.NonNullDiv(this, value(right))

    operator fun KExpression<UShort>.div(right: KExpression<UShort>): KNullableExpression<UShort> =
        BinaryOperatorExpression.NullableDiv(this, right)

    operator fun KExpression<UShort>.div(right: UShort): KNullableExpression<UShort> =
        BinaryOperatorExpression.NullableDiv(this, value(right))

    operator fun KNonNullExpression<UShort>.rem(right: KNonNullExpression<UShort>): KNonNullExpression<UShort> =
        BinaryOperatorExpression.NonNullRem(this, right)

    operator fun KNonNullExpression<UShort>.rem(right: UShort): KNonNullExpression<UShort> =
        BinaryOperatorExpression.NonNullRem(this, value(right))

    operator fun KExpression<UShort>.rem(right: KExpression<UShort>): KNullableExpression<UShort> =
        BinaryOperatorExpression.NullableRem(this, right)

    operator fun KExpression<UShort>.rem(right: UShort): KNullableExpression<UShort> =
        BinaryOperatorExpression.NullableRem(this, value(right))
}

object UnsignedByte {
    operator fun KNonNullExpression<UByte>.plus(right: KNonNullExpression<UByte>): KNonNullExpression<UByte> =
        BinaryOperatorExpression.NonNullPlus(this, right)

    operator fun KNonNullExpression<UByte>.plus(right: UByte): KNonNullExpression<UByte> =
        BinaryOperatorExpression.NonNullPlus(this, value(right))

    operator fun KExpression<UByte>.plus(right: KExpression<UByte>): KNullableExpression<UByte> =
        BinaryOperatorExpression.NullablePlus(this, right)

    operator fun KExpression<UByte>.plus(right: UByte): KNullableExpression<UByte> =
        BinaryOperatorExpression.NullablePlus(this, value(right))

    operator fun KNonNullExpression<UByte>.minus(right: KNonNullExpression<UByte>): KNonNullExpression<UByte> =
        BinaryOperatorExpression.NonNullMinus(this, right)

    operator fun KNonNullExpression<UByte>.minus(right: UByte): KNonNullExpression<UByte> =
        BinaryOperatorExpression.NonNullMinus(this, value(right))

    operator fun KExpression<UByte>.minus(right: KExpression<UByte>): KNullableExpression<UByte> =
        BinaryOperatorExpression.NullableMinus(this, right)

    operator fun KExpression<UByte>.minus(right: UByte): KNullableExpression<UByte> =
        BinaryOperatorExpression.NullableMinus(this, value(right))

    operator fun KNonNullExpression<UByte>.times(right: KNonNullExpression<UByte>): KNonNullExpression<UByte> =
        BinaryOperatorExpression.NonNullTimes(this, right)

    operator fun KNonNullExpression<UByte>.times(right: UByte): KNonNullExpression<UByte> =
        BinaryOperatorExpression.NonNullTimes(this, value(right))

    operator fun KExpression<UByte>.times(right: KExpression<UByte>): KNullableExpression<UByte> =
        BinaryOperatorExpression.NullableTimes(this, right)

    operator fun KExpression<UByte>.times(right: UByte): KNullableExpression<UByte> =
        BinaryOperatorExpression.NullableTimes(this, value(right))

    operator fun KNonNullExpression<UByte>.div(right: KNonNullExpression<UByte>): KNonNullExpression<UByte> =
        BinaryOperatorExpression.NonNullDiv(this, right)

    operator fun KNonNullExpression<UByte>.div(right: UByte): KNonNullExpression<UByte> =
        BinaryOperatorExpression.NonNullDiv(this, value(right))

    operator fun KExpression<UByte>.div(right: KExpression<UByte>): KNullableExpression<UByte> =
        BinaryOperatorExpression.NullableDiv(this, right)

    operator fun KExpression<UByte>.div(right: UByte): KNullableExpression<UByte> =
        BinaryOperatorExpression.NullableDiv(this, value(right))

    operator fun KNonNullExpression<UByte>.rem(right: KNonNullExpression<UByte>): KNonNullExpression<UByte> =
        BinaryOperatorExpression.NonNullRem(this, right)

    operator fun KNonNullExpression<UByte>.rem(right: UByte): KNonNullExpression<UByte> =
        BinaryOperatorExpression.NonNullRem(this, value(right))

    operator fun KExpression<UByte>.rem(right: KExpression<UByte>): KNullableExpression<UByte> =
        BinaryOperatorExpression.NullableRem(this, right)

    operator fun KExpression<UByte>.rem(right: UByte): KNullableExpression<UByte> =
        BinaryOperatorExpression.NullableRem(this, value(right))
}

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



infix fun KExpression<String>.like(
    pattern: String
): KNonNullExpression<Boolean> =
    LikePredicate(this, false, pattern, LikeMode.ANYWHERE)

infix fun KExpression<String>.`like?`(
    pattern: String?
): KNonNullExpression<Boolean>? =
    pattern?.takeIf { it.isNotEmpty() }?.let {
        LikePredicate(this, false, it, LikeMode.ANYWHERE)
    }

infix fun KExpression<String>.ilike(
    pattern: String
): KNonNullExpression<Boolean> =
    LikePredicate(this, true, pattern, LikeMode.ANYWHERE)

infix fun KExpression<String>.`ilike?`(
    pattern: String?
): KNonNullExpression<Boolean>? =
    pattern?.takeIf { it.isNotEmpty() }?.let {
        LikePredicate(this, true, it, LikeMode.ANYWHERE)
    }

fun KExpression<String>.like(
    pattern: String,
    mode: LikeMode
): KNonNullExpression<Boolean> =
    LikePredicate(this, false, pattern, mode)

fun KExpression<String>.`like?`(
    pattern: String?,
    mode: LikeMode
): KNonNullExpression<Boolean>? =
    pattern?.takeIf { it.isNotEmpty() || mode == LikeMode.EXACT }?.let {
        LikePredicate(this, false, it, mode)
    }

fun KExpression<String>.ilike(
    pattern: String,
    mode: LikeMode
): KNonNullExpression<Boolean> =
    LikePredicate(this, true, pattern, mode)

fun KExpression<String>.`ilike?`(
    pattern: String?,
    mode: LikeMode
): KNonNullExpression<Boolean>? =
    pattern?.takeIf { it.isNotEmpty() || mode == LikeMode.EXACT }?.let {
        LikePredicate(this, true, it, mode)
    }



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
    InCollectionPredicate(nullable = false, negative = false, this, values)

infix fun <T: Any> KExpression<T>.valueNotIn(
    values: Collection<T>
): KNonNullExpression<Boolean> =
    InCollectionPredicate(nullable = false, negative = true, this, values)

infix fun <T: Any> KExpression<T>.expressionIn(
    operands: Collection<KNonNullExpression<T>>
): KNonNullExpression<Boolean> =
    InExpressionCollectionPredicate(negative = false, this, operands)

infix fun <T: Any> KExpression<T>.expressionNotIn(
    operands: Collection<KNonNullExpression<T>>
): KNonNullExpression<Boolean> =
    InExpressionCollectionPredicate(negative = true, this, operands)

infix fun <T: Any> KExpression<T>.nullableValueIn(
    values: Collection<T?>
): KNonNullExpression<Boolean> =
    InCollectionPredicate(nullable = true, negative = false, this, values)

infix fun <T: Any> KExpression<T>.nullableValueNotIn(
    values: Collection<T?>
): KNonNullExpression<Boolean> =
    InCollectionPredicate(nullable = true, negative = true, this, values)

infix fun <T: Any> KExpression<T>.valueIn(
    subQuery: KTypedSubQuery<T>
): KNonNullExpression<Boolean> =
    InSubQueryPredicate(false, this, subQuery)

infix fun <T: Any> KExpression<T>.valueNotIn(
    subQuery: KTypedSubQuery<T>
): KNonNullExpression<Boolean> =
    InSubQueryPredicate(true, this, subQuery)



infix fun <T: Any> KExpression<T>.`valueIn?`(
    values: Collection<T>?
): KNonNullExpression<Boolean>? =
    values?.let {
        InCollectionPredicate(nullable = false, negative = false, this, it)
    }

infix fun <T: Any> KExpression<T>.`valueNotIn?`(
    values: Collection<T>?
): KNonNullExpression<Boolean>? =
    values?.let {
        InCollectionPredicate(nullable = false, negative = true, this, it)
    }

infix fun <T: Any> KExpression<T>.`nullableValueIn?`(
    values: Collection<T?>?
): KNonNullExpression<Boolean>? =
    values?.let {
        InCollectionPredicate(nullable = true, negative = false, this, it)
    }

infix fun <T: Any> KExpression<T>.`nullableValueNotIn?`(
    values: Collection<T?>?
): KNonNullExpression<Boolean>? =
    values?.let {
        InCollectionPredicate(nullable = true, negative = true, this, it)
    }



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
