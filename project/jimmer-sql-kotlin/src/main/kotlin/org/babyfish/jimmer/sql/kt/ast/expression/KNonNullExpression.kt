package org.babyfish.jimmer.sql.kt.ast.expression

import org.babyfish.jimmer.sql.ast.Selection

interface KNonNullExpression<T: Any> : KExpression<T>, Selection<T>