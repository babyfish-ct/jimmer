package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.ast.PropExpression
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.spi.KNonNullPropExpressionImplementor
import org.babyfish.jimmer.sql.meta.EmbeddedColumns
import org.babyfish.jimmer.sql.runtime.SqlBuilder
import kotlin.reflect.KProperty1

internal class NonNullPropExpressionImpl<T: Any>(
    internal val javaPropExpression: PropExpressionImpl<T>
) : AbstractKExpression<T>(), KNonNullPropExpressionImplementor<T>, PropExpressionImplementor<T> {

    override fun getType(): Class<T> =
        javaPropExpression.type

    override fun <X : Any> get(prop: KProperty1<T, X>): KNonNullPropExpression<X> =
        (javaPropExpression as? PropExpression.Embedded<*>)?.let {
            NonNullPropExpressionImpl(it.get(prop.name))
        } ?: error("The current property $javaPropExpression is not embedded property")

    override fun precedence(): Int =
        javaPropExpression.precedence()

    override fun accept(visitor: AstVisitor) {
        javaPropExpression.accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        javaPropExpression.renderTo(builder)
    }

    override fun renderTo(builder: SqlBuilder, ignoreEmbeddedTuple: Boolean) {
        javaPropExpression.renderTo(builder, ignoreEmbeddedTuple)
    }

    override fun getTable(): Table<*> =
        javaPropExpression.table

    override fun getProp(): ImmutableProp =
        javaPropExpression.prop

    override fun getPartial(): EmbeddedColumns.Partial? =
        javaPropExpression.partial
}