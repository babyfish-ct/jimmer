package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.query.impl.KConfigurableSubQueryImpl
import org.jetbrains.annotations.NotNull

interface KTypedSubQuery<R> {

    infix fun union(other: KTypedSubQuery<R>): KTypedSubQuery<R>

    infix fun unionAll(other: KTypedSubQuery<R>): KTypedSubQuery<R>

    infix operator fun minus(other: KTypedSubQuery<R>): KTypedSubQuery<R>

    infix fun intersect(other: KTypedSubQuery<R>): KTypedSubQuery<R>

    interface NonNull<R: Any>: KTypedSubQuery<R>, KNonNullExpression<R> {

        infix fun union(other: NonNull<R>): NonNull<R>

        infix fun union(other: Nullable<R>): Nullable<R>

        infix fun unionAll(other: NonNull<R>): NonNull<R>

        infix fun unionAll(other: Nullable<R>): Nullable<R>

        override infix operator fun minus(other: KTypedSubQuery<R>): NonNull<R>

        override infix fun intersect(other: KTypedSubQuery<R>): NonNull<R>
    }

    interface Nullable<R: Any>: KTypedSubQuery<R>, KNullableExpression<R> {

        override infix fun union(other: KTypedSubQuery<R>): Nullable<R>

        override infix fun unionAll(other: KTypedSubQuery<R>): Nullable<R>

        override infix operator fun minus(other: KTypedSubQuery<R>): Nullable<R>

        override infix fun intersect(other: KTypedSubQuery<R>): Nullable<R>
    }
}