package org.babyfish.jimmer.sql.kt.ast.expression

interface KExpression<T: Any> {

    fun asNonNull() = this as KNonNullExpression<T>
}