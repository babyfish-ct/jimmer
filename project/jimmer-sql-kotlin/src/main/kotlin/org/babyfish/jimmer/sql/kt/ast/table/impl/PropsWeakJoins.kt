package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinHandle
import org.babyfish.jimmer.sql.ast.table.WeakJoin
import org.babyfish.jimmer.sql.ast.table.spi.TableLike
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike
import org.babyfish.jimmer.sql.kt.ast.table.KPropsWeakJoin
import org.babyfish.jimmer.sql.kt.ast.table.KPropsWeakJoinFun
import org.babyfish.jimmer.sql.kt.ast.table.KPropsWeakJoinFunContextImpl

@Suppress("UNCHECKED_CAST")
fun <SP: KPropsLike, TP: KPropsLike> createPropsWeakJoinTable(
    sourceTable: SP,
    targetTableType: Class<TP>,
    propsWeakJoinFun: KPropsWeakJoinFun<SP, TP>,
    joinType: JoinType
): TP =
    if (sourceTable is KTableImplementor<*>) {
        sourceTable.javaTable.weakJoinImplementor<Any>(
            createPropsWeakJoinHandle(
                sourceTable::class.java,
                targetTableType::class.java,
                propsWeakJoinFun
            ),
            joinType
        ) as TP
    } else {
        TODO()
    }

fun <SP: KPropsLike, TP: KPropsLike> createPropsWeakJoinHandle(
    sourceType: Class<*>,
    targetType: Class<*>,
    propsWeakJoinFun: KPropsWeakJoinFun<SP, TP>
): WeakJoinHandle {
    val lambda = KPropsWeakJoinLambdaFactory(sourceType, targetType).get(propsWeakJoinFun)
        ?: throw IllegalArgumentException("The argument `weakJoinFun` must be lambda")
    val weakJoin: WeakJoin<TableLike<*>, TableLike<*>> = LambdaPropsWeakJoin(propsWeakJoinFun)
    return WeakJoinHandle.of(
        lambda,
        false,
        false,
        weakJoin
    )
}

private class LambdaPropsWeakJoin<SP: KPropsLike, TP: KPropsLike>(
    private val propsWeakJoinFun: KPropsWeakJoinFun<SP, TP>
) : KPropsWeakJoin<SP, TP>() {

    override fun on(
        source: SP,
        target: TP,
        ctx: Context<SP, TP>
    ): KNonNullExpression<Boolean> {
        val funCtx = KPropsWeakJoinFunContextImpl(source, target, ctx)
        return propsWeakJoinFun.run {
            funCtx.on()
        }
    }
}