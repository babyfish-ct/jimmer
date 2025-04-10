package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.sql.kt.ast.expression.impl.*
import org.babyfish.jimmer.sql.kt.ast.expression.impl.ConcatExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.LPadExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.LTrimExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.LowerExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.RPadExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.RTrimExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.TrimExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.UpperExpression

fun concat(vararg expressions: KNonNullExpression<String>): KNonNullExpression<String> =
    ConcatExpression.NonNull(expressions.toList())

fun concat(vararg expressions: KExpression<String>): KNullableExpression<String> =
    ConcatExpression.Nullable(expressions.toList())

fun KNonNullExpression<String>.upper(): KNonNullExpression<String> =
    UpperExpression.NonNull(this)

fun KNullableExpression<String>.upper(): KNullableExpression<String> =
    UpperExpression.Nullable(this)

fun KNonNullExpression<String>.lower(): KNonNullExpression<String> =
    LowerExpression.NonNull(this)

fun KNullableExpression<String>.lower(): KNullableExpression<String> =
    LowerExpression.Nullable(this)

fun KNonNullExpression<String>.trim(): KNonNullExpression<String> =
    TrimExpression.NonNull(this)

fun KNullableExpression<String>.trim(): KNullableExpression<String> =
    TrimExpression.Nullable(this)

fun KNonNullExpression<String>.ltrim(): KNonNullExpression<String> =
    LTrimExpression.NonNull(this)

fun KNullableExpression<String>.ltrim(): KNullableExpression<String> =
    LTrimExpression.Nullable(this)

fun KNonNullExpression<String>.rtrim(): KNonNullExpression<String> =
    RTrimExpression.NonNull(this)

fun KNullableExpression<String>.rtrim(): KNullableExpression<String> =
    RTrimExpression.Nullable(this)

fun KNonNullExpression<String>.lpad(length: Int, pad: String): KNonNullExpression<String> =
    LPadExpression.NonNull(this, value(length), value(pad))

fun KNullableExpression<String>.lpad(length: Int, pad: String): KNullableExpression<String> =
    LPadExpression.Nullable(this, value(length), value(pad))

fun KNonNullExpression<String>.lpad(length: KNonNullExpression<Int>, pad: String): KNonNullExpression<String> =
    LPadExpression.NonNull(this, length, value(pad))

fun KNullableExpression<String>.lpad(length: KNonNullExpression<Int>, pad: String): KNullableExpression<String> =
    LPadExpression.Nullable(this, length, value(pad))

fun KNonNullExpression<String>.rpad(length: Int, pad: String): KNonNullExpression<String> =
    RPadExpression.NonNull(this, value(length), value(pad))

fun KNullableExpression<String>.rpad(length: Int, pad: String): KNullableExpression<String> =
    RPadExpression.Nullable(this, value(length), value(pad))

fun KNonNullExpression<String>.rpad(length: KNonNullExpression<Int>, pad: String): KNonNullExpression<String> =
    RPadExpression.NonNull(this, length, value(pad))

fun KNullableExpression<String>.rpad(length: KNonNullExpression<Int>, pad: String): KNullableExpression<String> =
    RPadExpression.Nullable(this, length, value(pad))

fun KNonNullExpression<String>.position(substring: String, start: Int? = null): KNonNullExpression<Int> =
    PositionExpression.NonNull(value(substring), this, start?.let { value(it) })

fun KNullableExpression<String>.position(substring: String, start: Int? = null): KNullableExpression<Int> =
    PositionExpression.Nullable(value(substring), this, start?.let { value(it) })

fun KNonNullExpression<String>.position(substring: KNonNullExpression<String>, start: Int? = null): KNonNullExpression<Int> =
    PositionExpression.NonNull(substring, this, start?.let { value(it) })

fun KNullableExpression<String>.position(substring: KNonNullExpression<String>, start: Int? = null): KNullableExpression<Int> =
    PositionExpression.Nullable(substring, this, start?.let { value(it) })

fun KNonNullExpression<String>.left(length: Int): KNonNullExpression<String> =
    LeftExpression.NonNull(this, value(length))

fun KNullableExpression<String>.left(length: Int): KNullableExpression<String> =
    LeftExpression.Nullable(this, value(length))

fun KNonNullExpression<String>.left(length: KNonNullExpression<Int>): KNonNullExpression<String> =
    LeftExpression.NonNull(this, length)

fun KNullableExpression<String>.left(length: KNonNullExpression<Int>): KNullableExpression<String> =
    LeftExpression.Nullable(this, length)

fun KNonNullExpression<String>.right(length: Int): KNonNullExpression<String> =
    RightExpression.NonNull(this, value(length))

fun KNullableExpression<String>.right(length: Int): KNullableExpression<String> =
    RightExpression.Nullable(this, value(length))

fun KNonNullExpression<String>.right(length: KNonNullExpression<Int>): KNonNullExpression<String> =
    RightExpression.NonNull(this, length)

fun KNullableExpression<String>.right(length: KNonNullExpression<Int>): KNullableExpression<String> =
    RightExpression.Nullable(this, length)

fun KNonNullExpression<String>.substring(start: Int, len: Int = 1): KNonNullExpression<String> =
    SubstringExpression.NonNull(
        this,
        value(start),
        len.takeIf { it != 1 }?.let { value(it) }
    )

fun KNullableExpression<String>.substring(start: Int, len: Int = 1): KNullableExpression<String> =
    SubstringExpression.Nullable(
        this,
        value(start),
        len.takeIf { it != 1 }?.let { value(it) }
    )

fun KNonNullExpression<String>.substring(start: KNonNullExpression<Int>, len: KNonNullExpression<Int>? = null): KNonNullExpression<String> =
    SubstringExpression.NonNull(this, start, len)

fun KNullableExpression<String>.substring(start: KNonNullExpression<Int>, len: KNonNullExpression<Int>? = null): KNullableExpression<String> =
    SubstringExpression.Nullable(this, start, len)