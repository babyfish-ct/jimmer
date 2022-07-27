package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression

interface KTypedSubQuery<R> {

    interface NonNull<R: Any>: KTypedSubQuery<R>, KNonNullExpression<R>

    interface Nullable<R: Any>: KTypedSubQuery<R>, KNullableExpression<R>
}