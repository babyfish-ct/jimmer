package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.Predicate
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.PredicateImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class AbstractKPredicate :
    AbstractKExpression<Boolean>(),
    KNonNullExpression<Boolean>,
    PredicateImplementor {

    override fun getType(): Class<Boolean> =
        Boolean::class.java

    abstract override fun not(): AbstractKPredicate
}

internal fun KNonNullExpression<Boolean>.toJavaPredicate(): Predicate =
    if (this is Predicate) {
        this
    } else {
        PredicateWrapper(this)
    }

internal fun KNonNullExpression<Boolean>.toKtPredicateImpl(): AbstractKPredicate =
    if (this is AbstractKPredicate) {
        this
    } else {
        PredicateWrapper(this)
    }

internal class PredicateWrapper(
    private val expr: KNonNullExpression<Boolean>
) : AbstractKPredicate() {

    override fun not(): AbstractKPredicate =
        NotPredicate(this)

    @Suppress("UNCHECKED_CAST")
    override fun precedence(): Int =
        (expr as ExpressionImplementor<Boolean>).precedence()

    override fun accept(visitor: AstVisitor) =
        (expr as Ast).accept(visitor)

    override fun renderTo(builder: SqlBuilder) =
        (expr as Ast).renderTo(builder)
}