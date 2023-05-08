package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionPrecedences
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class CompositePredicate(
    protected val predicates: List<AbstractKPredicate>
): AbstractKPredicate() {

    override fun accept(visitor: AstVisitor) {
        for (predicate in predicates) {
            predicate.accept(visitor)
        }
    }

    override fun renderTo(builder: SqlBuilder) {
        builder.enter(scopeType())
        for (predicate in predicates) {
            builder.separator()
            renderChild(predicate as Ast, builder)
        }
        builder.leave()
    }

    protected abstract fun scopeType(): SqlBuilder.ScopeType
}

internal class AndPredicate(
    predicates: List<AbstractKPredicate>
): CompositePredicate(predicates) {

    override fun scopeType(): SqlBuilder.ScopeType =
        SqlBuilder.ScopeType.AND

    override fun not(): AbstractKPredicate =
        OrPredicate(predicates.map { it.not() })

    override fun precedence(): Int = ExpressionPrecedences.AND
}

internal class OrPredicate(
    predicates: List<AbstractKPredicate>
): CompositePredicate(predicates) {

    override fun scopeType(): SqlBuilder.ScopeType =
        SqlBuilder.ScopeType.OR

    override fun not(): AbstractKPredicate =
        AndPredicate(predicates.map { it.not() })

    override fun precedence(): Int = ExpressionPrecedences.OR
}

internal class NotPredicate(
    private val predicate: AbstractKPredicate
) : AbstractKPredicate() {

    override fun not(): AbstractKPredicate = predicate

    override fun precedence(): Int = ExpressionPrecedences.NOT

    override fun accept(visitor: AstVisitor) {
        predicate.accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        builder.sql("not ")
        usingLowestPrecedence {
            renderChild(predicate, builder)
        }
    }
}