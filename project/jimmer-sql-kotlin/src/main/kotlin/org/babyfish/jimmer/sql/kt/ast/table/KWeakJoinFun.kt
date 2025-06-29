package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import java.io.Serializable

/**
 * Note: Although this lambda interface implements the
 * `Serializable` interface, it has nothing to do with
 * serialization functionality.
 *
 * The purpose of this is to ensure that the interface
 * is classified as a Serializable lambda, forcing the
 * compiler to generate runtime-readable bytecode for
 * such lambda expressions. This allows jimmer to determine
 * whether the logic of two lambdas is equivalent.
 */
fun interface KWeakJoinFun<S: Any, T: Any> : Serializable {

    fun KWeakJoinFunContext<S, T>.on(): KNonNullExpression<Boolean>
}

interface KWeakJoinFunContext<S: Any, T: Any> : KWeakJoin.Context<S, T> {
    val source: KNonNullTable<S>
    val target: KNonNullTable<T>
}

internal class KWeakJoinFunContextImpl<S: Any, T: Any>(
    override val source: KNonNullTable<S>,
    override val target: KNonNullTable<T>,
    private val ctx: KWeakJoin.Context<S, T>
) : KWeakJoinFunContext<S, T>, KWeakJoin.Context<S, T> by (ctx)