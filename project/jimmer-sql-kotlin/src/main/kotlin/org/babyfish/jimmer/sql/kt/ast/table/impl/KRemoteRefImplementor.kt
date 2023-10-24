package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.table.KRemoteRef

interface KRemoteRefImplementor<E: Any> : KRemoteRef<E> {

    fun <X: Any> id(): KPropExpression<X>
}