package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.EmbeddableDto
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.ast.PropExpression
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullEmbeddedPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class NonNullEmbeddedPropExpressionImpl<T: Any>(
    javaPropExpression: PropExpressionImpl.EmbeddedImpl<T>
) : NonNullPropExpressionImpl<T>(javaPropExpression), KNonNullEmbeddedPropExpression<T> {

    override fun <X : Any> get(prop: KProperty1<T, X?>): KPropExpression<X> =
        kotlinExpr((javaPropExpression as PropExpressionImpl.EmbeddedImpl<*>).get(prop.name))

    override fun <X : Any> get(prop: ImmutableProp): KPropExpression<X> =
        kotlinExpr((javaPropExpression as PropExpressionImpl.EmbeddedImpl<*>).get(prop.name))

    override fun fetch(fetcher: Fetcher<T>?): Selection<T> =
        (javaPropExpression as PropExpressionImpl.EmbeddedImpl<T>).fetch(fetcher)

    override fun <V : EmbeddableDto<T>> fetch(valueType: KClass<V>): Selection<V> =
        (javaPropExpression as PropExpressionImpl.EmbeddedImpl<T>).fetch(valueType.java)

    companion object {
        private fun <X: Any> kotlinExpr(
            javaExpr: PropExpressionImpl<X>
        ): KPropExpression<X> =
            if (javaExpr is PropExpression.Embedded<*>) {
                if (javaExpr.deepestProp.isNullable) {
                    NullableEmbeddedPropExpressionImpl(javaExpr as PropExpressionImpl.EmbeddedImpl<X>)
                } else {
                    NonNullEmbeddedPropExpressionImpl(javaExpr as PropExpressionImpl.EmbeddedImpl<X>)
                }
            } else {
                if (javaExpr.deepestProp.isNullable) {
                    NullablePropExpressionImpl(javaExpr)
                } else {
                    NonNullPropExpressionImpl(javaExpr)
                }
            }
    }
}