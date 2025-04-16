package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.ast.impl.PredicateImplementor
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection
import org.babyfish.jimmer.sql.kt.ast.expression.impl.AndPredicate
import org.babyfish.jimmer.sql.kt.ast.expression.impl.BetweenPredicate
import org.babyfish.jimmer.sql.kt.ast.expression.impl.ComparisonPredicate
import org.babyfish.jimmer.sql.kt.ast.expression.impl.InCollectionPredicate
import org.babyfish.jimmer.sql.kt.ast.expression.impl.InExpressionCollectionPredicate
import org.babyfish.jimmer.sql.kt.ast.expression.impl.InSubQueryPredicate
import org.babyfish.jimmer.sql.kt.ast.expression.impl.IsNotNullPredicate
import org.babyfish.jimmer.sql.kt.ast.expression.impl.IsNullPredicate
import org.babyfish.jimmer.sql.kt.ast.expression.impl.JavaToKotlinPredicateWrapper
import org.babyfish.jimmer.sql.kt.ast.expression.impl.LikePredicate
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.OrPredicate
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toKotlinPredicate
import org.babyfish.jimmer.sql.kt.ast.query.KExample
import org.babyfish.jimmer.sql.kt.ast.query.KTypedSubQuery
import org.babyfish.jimmer.sql.kt.ast.query.example
import org.babyfish.jimmer.sql.kt.ast.query.viewExample
import org.babyfish.jimmer.sql.kt.ast.table.KTable
import org.babyfish.jimmer.sql.kt.ast.table.impl.KTableImplementor

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