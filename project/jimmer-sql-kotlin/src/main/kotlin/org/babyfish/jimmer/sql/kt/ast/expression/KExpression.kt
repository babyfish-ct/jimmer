package org.babyfish.jimmer.sql.kt.ast.expression

interface KExpression<T> {

    fun asNonNull() = this as KNonNullExpression<T>
}