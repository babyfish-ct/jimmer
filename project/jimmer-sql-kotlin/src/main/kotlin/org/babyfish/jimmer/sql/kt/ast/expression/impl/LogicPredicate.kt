package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.Predicate
import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstContext
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionPrecedences
import org.babyfish.jimmer.sql.ast.impl.associated.VirtualPredicate
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.not
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class CompositePredicate(
    protected val predicates: List<KNonNullExpression<Boolean>>
): AbstractKPredicate() {

    override fun accept(visitor: AstVisitor) {
        for (predicate in predicates) {
            (predicate as Ast).accept(visitor)
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

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(predicates)

    protected abstract fun scopeType(): AbstractSqlBuilder.ScopeType
}

internal class AndPredicate(
    predicates: List<KNonNullExpression<Boolean>>
): CompositePredicate(predicates) {

    override fun scopeType(): AbstractSqlBuilder.ScopeType =
        AbstractSqlBuilder.ScopeType.AND

    override fun not(): Predicate =
        OrPredicate(predicates.map { it.not() })

    override fun precedence(): Int = ExpressionPrecedences.AND

    @Suppress("UNCHECKED_CAST")
    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? {
        ctx.pushVirtualPredicateContext(VirtualPredicate.Op.AND)
        val list = ctx.resolveVirtualPredicates(predicates).filterNotNull() as List<Predicate>
        ctx.popVirtualPredicateContext()
        return when (list.size) {
            0 -> null
            1 -> list[0]
            else -> AndPredicate(list.map { it.toKotlinPredicate() })
        } as Ast?
    }
}

internal class OrPredicate(
    predicates: List<KNonNullExpression<Boolean>>
): CompositePredicate(predicates) {

    override fun scopeType(): AbstractSqlBuilder.ScopeType =
        AbstractSqlBuilder.ScopeType.OR

    override fun not(): AbstractKPredicate =
        AndPredicate(predicates.map { it.not() })

    override fun precedence(): Int = ExpressionPrecedences.OR

    @Suppress("UNCHECKED_CAST")
    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? {
        ctx.pushVirtualPredicateContext(VirtualPredicate.Op.OR)
        val list = ctx.resolveVirtualPredicates(predicates).filterNotNull() as List<Predicate>
        ctx.popVirtualPredicateContext()
        return when (list.size) {
            0 -> null
            1 -> list[0]
            else -> OrPredicate(list.map { it.toKotlinPredicate() })
        } as Ast?
    }
}

internal class NotPredicate(
    private val predicate: KNonNullExpression<Boolean>
) : AbstractKPredicate() {

    override fun not(): Predicate = predicate.toJavaPredicate()

    override fun precedence(): Int = ExpressionPrecedences.NOT

    override fun accept(visitor: AstVisitor) {
        (predicate as Ast).accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        builder.sql("not ")
        usingLowestPrecedence {
            renderChild(predicate as Ast, builder)
        }
    }

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(predicate)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? {
        ctx.pushVirtualPredicateContext(VirtualPredicate.Op.AND)
        val newPredicate = ctx.resolveVirtualPredicate(predicate)
        ctx.popVirtualPredicateContext()
        return when {
            newPredicate === null -> null
            newPredicate === predicate -> this
            else -> NotPredicate(newPredicate)
        }
    }
}