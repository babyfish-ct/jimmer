package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.ast.PropExpression
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.TupleExpressionImplementor
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.runtime.ExecutionException
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor
import org.babyfish.jimmer.sql.runtime.ScalarProvider
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal class LiteralExpression<T: Any>(
    private val value: T
) : AbstractKExpression<T>(), KNonNullExpression<T> {

    private var matchedProp: ImmutableProp? = null

    private var matchedProps: Array<ImmutableProp?>? = null

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<T> = value::class.java as Class<T>

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {}

    override fun renderTo(builder: SqlBuilder) {
        val sqlClient = builder.astContext.sqlClient
        if (matchedProp !== null) {
            val scalarProvider = sqlClient.getScalarProvider<Any, Any>(matchedProp)
            if (scalarProvider !== null) {
                builder.variable(
                    try {
                        scalarProvider.toSql(value)
                    } catch (ex: Exception) {
                        throw ExecutionException(
                            "Cannot convert the value\"" +
                                value +
                                "\" of prop \"" +
                                matchedProp +
                                "\" by scalar provider \"" +
                                scalarProvider::class.qualifiedName +
                                "\"",
                            ex
                        )
                    }
                )
                return
            }
        } else if (matchedProps !== null) {
            val scalarProviders = matchedProps!!
                .map { sqlClient.getScalarProvider<Any, Any>(it) }
                .takeIf { it.any { v -> v !== null } }
            if (scalarProviders !== null) {
                val newTuple = (value as TupleImplementor).convert { value, index ->
                    try {
                        scalarProviders[index]?.toSql(value) ?: value
                    } catch (ex: Exception) {
                        throw ExecutionException(
                            "Cannot convert the tuple item[" +
                                index +
                                "] of prop \"" +
                                matchedProps!![index] +
                                "\" by scalar provider \"" +
                                scalarProviders[index]::class.qualifiedName +
                                "\"",
                            ex
                        )
                    }
                }
                builder.variable(newTuple)
                return
            }
        }
        builder.variable(value)
    }

    companion object {

        @JvmStatic
        fun bind(mayBeLiteral: KExpression<*>, expression: KExpression<*>) {
            if (mayBeLiteral !is LiteralExpression<*>) {
                return
            }
            if (expression is KPropExpression<*>) {
                val propExpr = expression as PropExpressionImplementor<*>
                if (mayBeLiteral.matchedProp !== null && mayBeLiteral.matchedProp !== propExpr.prop) {
                    throw IllegalStateException(
                        "The matched property of the current literal has already been set, " +
                            "is the current literal expression is shared by difference parts of SQL DSL"
                    )
                }
                mayBeLiteral.matchedProp = propExpr.prop
            } else if (expression is TupleExpressionImplementor<*>) {
                val props =
                    (0 until expression.size())
                        .map { (expression[it] as? PropExpressionImplementor<*>)?.prop }
                        .takeIf { it.any { v-> v !== null } }
                        ?.toTypedArray() ?: return
                if (mayBeLiteral.matchedProps !== null && mayBeLiteral.matchedProps != props) {
                    throw IllegalStateException(
                        "The matched properties of the current literal has already been set, " +
                            "is the current literal expression is shared by difference parts of SQL DSL"
                    )
                }
                mayBeLiteral.matchedProps = props
            }
        }

        @JvmStatic
        fun convert(literals: Collection<*>, expression: KExpression<*>, sqlClient: JSqlClientImplementor): Collection<*> {
            if (literals.isEmpty()) {
                return literals
            }
            return when (expression) {
                is PropExpression<*> -> {
                    val prop = (expression as PropExpressionImplementor<*>).prop
                    val scalarProvider = sqlClient.getScalarProvider<Any, Any>(prop) ?: return literals
                    val newLiterals: MutableList<Any?> = ArrayList(literals.size)
                    for (literal in literals) {
                        try {
                            newLiterals.add(literal?.let { scalarProvider.toSql(it) })
                        } catch (ex: Exception) {
                            throw ExecutionException(
                                "Cannot convert the value \"" +
                                    literal +
                                    "\" of prop \"" +
                                    prop +
                                    "\" by scalar provider \"" +
                                    scalarProvider.javaClass.name +
                                    "\"",
                                ex
                            )
                        }
                    }
                    newLiterals
                }
                is TupleExpressionImplementor<*> -> {
                    val size = expression.size()
                    val props = arrayOfNulls<ImmutableProp>(size)
                    val scalarProviders = arrayOfNulls<ScalarProvider<Any, Any>?>(size)
                    var hasScalarProvider = false
                    for (i in 0 until size) {
                        val expr = expression[i]
                        if (expr is PropExpression<*>) {
                            val prop = (expr as PropExpressionImplementor<*>).prop
                            val scalarProvider = sqlClient.getScalarProvider<Any, Any>(prop)
                            if (scalarProvider != null) {
                                props[i] = prop
                                scalarProviders[i] = scalarProvider
                                hasScalarProvider = true
                            }
                        }
                    }
                    if (!hasScalarProvider) {
                        return literals
                    }
                    val newLiterals: MutableList<Any> = ArrayList(literals.size)
                    for (literal in literals) {
                        newLiterals.add(
                            (literal as TupleImplementor).convert { value: Any?, index: Int? ->
                                if (value != null) {
                                    val scalarProvider =
                                        scalarProviders[index!!]
                                    if (scalarProvider != null) {
                                        try {
                                            return@convert scalarProvider.toSql(value)
                                        } catch (ex: Exception) {
                                            throw ExecutionException(
                                                "Cannot convert the tuple item[" +
                                                    "index" +
                                                    "] of prop \"" +
                                                    props[index] +
                                                    "\" by scalar provider \"" +
                                                    scalarProvider.javaClass.name +
                                                    "\"",
                                                ex
                                            )
                                        }
                                    }
                                }
                                value
                            }
                        )
                    }
                    newLiterals
                }
                else -> literals
            }
        }
    }
}

internal class NullExpression<T: Any>(
    private val type: Class<T>
): AbstractKExpression<T>(), KNullableExpression<T> {

    override fun getType(): Class<T> = type

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {}

    override fun renderTo(builder: SqlBuilder) {
        builder.nullVariable(type)
    }
}

internal class ConstantExpression<T: Number>(
    private val value: T
) : AbstractKExpression<T>(), KNonNullExpression<T> {

    @Suppress("UNCHECKED_CAST")
    override fun getType(): Class<T> = value::class.java as Class<T>

    override fun precedence(): Int = 0

    override fun accept(visitor: AstVisitor) {}

    override fun renderTo(builder: SqlBuilder) {
        builder.sql(value.toString())
    }
}