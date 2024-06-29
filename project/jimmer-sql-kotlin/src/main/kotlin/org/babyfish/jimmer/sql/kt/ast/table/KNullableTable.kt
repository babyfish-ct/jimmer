package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.View
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.KNullablePropExpression
import kotlin.reflect.KClass

interface KNullableTable<E: Any> : KTable<E>, KNullableProps<E>, Selection<E?> {

    fun fetch(fetcher: Fetcher<E>?): Selection<E?>

    fun <S: View<E>> fetch(staticType: KClass<S>): Selection<S?>

    override fun <X : Any> get(prop: String): KNullablePropExpression<X>

    override fun <X : Any> get(prop: ImmutableProp): KNullablePropExpression<X>

    override fun <X : Any> getId(): KNullablePropExpression<X>

    override fun <X : Any> getAssociatedId(prop: String): KNullablePropExpression<X>

    override fun <X : Any> getAssociatedId(prop: ImmutableProp): KNullablePropExpression<X>

    override fun asTableEx(): KNullableTableEx<E>
}