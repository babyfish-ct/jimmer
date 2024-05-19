package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.sql.ast.impl.Ast

interface KExpression<T: Any>

internal fun KExpression<*>.toAst(): Ast =
    this as Ast