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
fun interface KPropsWeakJoinFun<SP: KPropsLike, TP: KPropsLike> : Serializable {

    fun KPropsWeakJoinFunContext<SP, TP>.on(): KNonNullExpression<Boolean>
}

interface KPropsWeakJoinFunContext<SP: KPropsLike, TP: KPropsLike> : KPropsWeakJoin.Context<SP, TP> {
    val source: SP
    val target: TP
}

internal class KPropsWeakJoinFunContextImpl<SP: KPropsLike, TP: KPropsLike>(
    override val source: SP,
    override val target: TP,
    private val ctx: KPropsWeakJoin.Context<SP, TP>
) : KPropsWeakJoinFunContext<SP, TP>, KPropsWeakJoin.Context<SP, TP> by (ctx)