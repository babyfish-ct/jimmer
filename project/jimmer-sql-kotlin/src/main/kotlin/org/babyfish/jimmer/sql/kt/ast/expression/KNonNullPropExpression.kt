package org.babyfish.jimmer.sql.kt.ast.expression

interface KNonNullPropExpression<T: Any> : KPropExpression<T>, KNonNullExpression<T>