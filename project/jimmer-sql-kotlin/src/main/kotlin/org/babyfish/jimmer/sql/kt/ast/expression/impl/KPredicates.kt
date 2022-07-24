package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression

fun and(vararg predicates: KNonNullExpression<Boolean>): KNonNullExpression<Boolean> {
    TODO()
}

fun or(vararg predicates: KNonNullExpression<Boolean>): KNonNullExpression<Boolean> {
    TODO()
}

fun KNonNullExpression<Boolean>.not(): KNonNullExpression<Boolean> {
    TODO()
}

