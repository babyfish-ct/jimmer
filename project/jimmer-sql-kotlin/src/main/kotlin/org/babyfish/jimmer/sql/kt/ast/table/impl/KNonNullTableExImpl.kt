package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.KNonNullPropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.KNullablePropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KNullableTableEx
import org.babyfish.jimmer.sql.runtime.SqlBuilder
import kotlin.reflect.KClass

internal class KNonNullTableExImpl<E: Any>(
    private val javaTable: TableImplementor<E>
) : Ast, KNonNullTableEx<E>, TableSelection<E> by (javaTable) {

    @Suppress("UNCHECKED_CAST")
    override fun <X : Any, EXP : KPropExpression<X>> get(prop: String): EXP =
        javaTable.get<PropExpressionImpl<X>>(prop).let {
            if (it.prop.isNullable) {
                KNullablePropExpressionImpl(it)
            } else {
                KNonNullPropExpressionImpl(it)
            }
        } as EXP

    override fun <X : Any> join(prop: String): KNonNullTableEx<X> =
        KNonNullTableExImpl(javaTable.join(prop))

    override fun <X : Any> outerJoin(prop: String): KNullableTableEx<X> =
        KNullableTableExImpl(javaTable.join(prop, JoinType.LEFT))

    override fun <X : Any> inverseJoin(targetType: KClass<X>, backProp: String): KNonNullTableEx<X> =
        KNonNullTableExImpl(javaTable.inverseJoin(targetType.java, backProp))

    override fun <X : Any> inverseOuterJoin(targetType: KClass<X>, backProp: String): KNullableTableEx<X> =
        KNullableTableExImpl(javaTable.inverseJoin(targetType.java, backProp))

    override fun accept(visitor: AstVisitor) {
        javaTable.accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        javaTable.renderTo(builder)
    }
}