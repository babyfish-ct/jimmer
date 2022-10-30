package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.ast.impl.table.TableSelection
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.isNotNull
import org.babyfish.jimmer.sql.kt.ast.expression.isNull

interface KTable<E: Any> : KProps<E> {
    fun asTableEx(): KTableEx<E>
}

fun KTable<*>.isNull(): KNonNullExpression<Boolean> {
    val idProp = (this as TableSelection).immutableType.idProp
    return get<Any, KPropExpression<Any>>(idProp.name).isNull()
}

fun KTable<*>.isNotNull(): KNonNullExpression<Boolean> {
    val idProp = (this as TableSelection).immutableType.idProp
    return get<Any, KPropExpression<Any>>(idProp.name).isNotNull()
}