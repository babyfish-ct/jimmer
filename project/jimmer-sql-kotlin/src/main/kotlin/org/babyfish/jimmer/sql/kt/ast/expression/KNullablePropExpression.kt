package org.babyfish.jimmer.sql.kt.ast.expression

interface KNullablePropExpression<T: Any> : KPropExpression<T>, KNullableExpression<T>