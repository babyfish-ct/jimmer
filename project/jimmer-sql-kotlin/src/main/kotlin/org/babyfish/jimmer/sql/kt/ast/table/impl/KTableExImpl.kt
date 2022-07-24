package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection
import org.babyfish.jimmer.sql.kt.ast.table.KNullableTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KTableEx
import org.babyfish.jimmer.sql.runtime.SqlBuilder
import kotlin.reflect.KClass

internal abstract class KTableExImpl<E: Any>(
    protected val javaTable: TableImplementor<E>
): Ast, KTableEx<E>, TableSelection<E> by(javaTable) {

    override fun <X : Any> outerJoin(prop: String): KNullableTableEx<X> =
        KNullableTableExImpl(javaTable.join(prop, JoinType.LEFT))

    override fun <X : Any> inverseOuterJoin(targetType: KClass<X>, backProp: String): KNullableTableEx<X> =
        KNullableTableExImpl(javaTable.inverseJoin(targetType.java, backProp))

    override fun accept(visitor: AstVisitor) {
        javaTable.accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        javaTable.renderTo(builder)
    }
}