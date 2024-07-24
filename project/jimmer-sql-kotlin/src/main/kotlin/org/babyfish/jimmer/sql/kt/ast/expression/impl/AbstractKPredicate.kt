package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.Predicate
import org.babyfish.jimmer.sql.ast.impl.*
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class AbstractKPredicate :
    AbstractKExpression<Boolean>(),
    KNonNullExpression<Boolean>,
    PredicateImplementor {

    override fun getType(): Class<Boolean> =
        Boolean::class.java

    abstract override fun not(): Predicate
}

internal fun KNonNullExpression<Boolean>.toJavaPredicate(): PredicateImplementor =
    if (this is PredicateImplementor) {
        this
    } else {
        KotlinToJavaPredicate(this)
    }

@Suppress("UNCHECKED_CAST")
internal fun Predicate.toKotlinPredicate(): KNonNullExpression<Boolean> =
    if (this is KNonNullExpression<*>) {
        this as KNonNullExpression<Boolean>
    } else {
        JavaToKotlinPredicateWrapper(this)
    }

internal class KotlinToJavaPredicate(
    private val expr: KNonNullExpression<Boolean>
) : AbstractKPredicate(), KNonNullExpression<Boolean> {

    override fun not(): AbstractKPredicate =
        NotPredicate(this)

    override fun precedence(): Int =
        (expr as ExpressionImplementor<*>).precedence()

    override fun accept(visitor: AstVisitor) =
        (expr as Ast).accept(visitor)

    override fun renderTo(builder: SqlBuilder) =
        (expr as Ast).renderTo(builder)

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(expr)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? =
        ctx.resolveVirtualPredicate(expr)?.let {
            KotlinToJavaPredicate(it)
        }
}

internal class JavaToKotlinPredicateWrapper(
    private val predicate: Predicate
) : AbstractKPredicate(), KNonNullExpression<Boolean>, PredicateWrapper {

    override fun not(): AbstractKPredicate =
        NotPredicate(this)

    override fun precedence(): Int =
        (predicate as ExpressionImplementor<*>).precedence()

    override fun accept(visitor: AstVisitor) =
        (predicate as Ast).accept(visitor)

    override fun renderTo(builder: SqlBuilder) =
        (predicate as Ast).renderTo(builder)

    override fun determineHasVirtualPredicate(): Boolean =
        hasVirtualPredicate(predicate)

    override fun onResolveVirtualPredicate(ctx: AstContext): Ast? =
        ctx.resolveVirtualPredicate(predicate)?.let {
            JavaToKotlinPredicateWrapper(it)
        }

    override fun unwrap(): Any = predicate

    override fun wrap(unwrapped: Any): Any =
        JavaToKotlinPredicateWrapper(unwrapped as Predicate)
}