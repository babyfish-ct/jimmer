package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullablePropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.table.KNullableTableEx
import kotlin.reflect.KClass

internal class KNullableTableExImpl<E: Any>(
    javaTable: TableImplementor<E>
) : KTableExImpl<E>(javaTable), KNullableTableEx<E> {

    @Suppress("UNCHECKED_CAST")
    override fun <X : Any, EXP : KPropExpression<X>> get(prop: String): EXP =
        NullablePropExpressionImpl(javaTable.get<PropExpressionImpl<X>>(prop)) as EXP

    override fun <X : Any> join(prop: String): KNullableTableEx<X> =
        KNullableTableExImpl(javaTable.join(prop))

    override fun <X: Any> inverseJoin(targetType: KClass<X>, backProp: String): KNullableTableEx<X> =
        KNullableTableExImpl(javaTable.inverseJoin(targetType.java, backProp))
}