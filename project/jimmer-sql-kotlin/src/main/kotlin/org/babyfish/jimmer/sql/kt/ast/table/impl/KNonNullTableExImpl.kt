package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NonNullPropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullablePropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import kotlin.reflect.KClass

internal class KNonNullTableExImpl<E: Any>(
    javaTable: TableImplementor<E>
) : KTableExImpl<E>(javaTable), KNonNullTableEx<E> {

    @Suppress("UNCHECKED_CAST")
    override fun <X : Any, EXP : KPropExpression<X>> get(prop: String): EXP =
        javaTable.get<PropExpressionImpl<X>>(prop).let {
            if (it.prop.isNullable) {
                NullablePropExpressionImpl(it)
            } else {
                NonNullPropExpressionImpl(it)
            }
        } as EXP

    override fun <X : Any> join(prop: String): KNonNullTableEx<X> =
        KNonNullTableExImpl(javaTable.join(prop))

    override fun <X: Any> inverseJoin(targetType: KClass<X>, backProp: String): KNonNullTableEx<X> =
        KNonNullTableExImpl(javaTable.inverseJoin(targetType.java, backProp))

    override fun fetch(fetcher: Fetcher<E>): Selection<E> =
        javaTable.fetch(fetcher)
}