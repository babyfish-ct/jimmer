package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.EmbeddableDto
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.ast.PropExpression
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableEmbeddedPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullablePropExpression
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class NullableEmbeddedPropExpressionImpl<T: Any>(
    javaProp: PropExpressionImpl.EmbeddedImpl<T>
) : NullablePropExpressionImpl<T>(javaProp), KNullableEmbeddedPropExpression<T> {

    override fun <X : Any> get(prop: KProperty1<T, X?>): KNullablePropExpression<X> =
        kotlinExpr((javaPropExpression as PropExpressionImpl.EmbeddedImpl<*>).get(prop.name))

    override fun <X : Any> get(prop: ImmutableProp): KNullablePropExpression<X> =
        kotlinExpr((javaPropExpression as PropExpressionImpl.EmbeddedImpl<*>).get(prop.name))

    override fun fetch(fetcher: Fetcher<T>?): Selection<T?> =
        (javaPropExpression as PropExpressionImpl.EmbeddedImpl<T>).fetch(fetcher)

    override fun <V : EmbeddableDto<T>> fetch(valueType: KClass<V>): Selection<V?> =
        (javaPropExpression as PropExpressionImpl.EmbeddedImpl<T>).fetch(valueType.java)

    companion object {
        private fun <X:Any> kotlinExpr(
            javaExpr: PropExpressionImpl<X>
        ): KNullablePropExpression<X> =
            if (javaExpr is PropExpression.Embedded<*>) {
                NullableEmbeddedPropExpressionImpl(javaExpr as PropExpressionImpl.EmbeddedImpl<X>)
            } else {
                NullablePropExpressionImpl(javaExpr)
            }
    }
}