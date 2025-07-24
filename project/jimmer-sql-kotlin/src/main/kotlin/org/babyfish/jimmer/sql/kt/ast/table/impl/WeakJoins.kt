package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinHandle
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.ast.table.WeakJoin
import org.babyfish.jimmer.sql.ast.table.spi.TableLike
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.table.*

@Suppress("UNCHECKED_CAST")
fun <S: Any, T: Any> createWeakJoinTable(
    javaTable: TableImplementor<S>,
    targetType: Class<T>,
    weakJoinFun: KWeakJoinFun<S, T>,
    joinType: JoinType
): TableImplementor<T> =
    javaTable.weakJoinImplementor(
        createWeakJoinHandle(
            javaTable.immutableType.javaClass as Class<S>,
            targetType,
            weakJoinFun
        ),
        joinType
    )

@Suppress("UNCHECKED_CAST")
fun <S: Any, T: Any> createWeakJoinHandle(
    sourceType: Class<S>,
    targetType: Class<T>,
    weakJoinFun: KWeakJoinFun<S, T>
): WeakJoinHandle {
    val lambda = KWeakJoinLambdaFactory(sourceType, targetType).get(weakJoinFun)
        ?: throw IllegalArgumentException("The argument `weakJoinFun` must be lambda")
    val weakJoin: WeakJoin<Table<S>, Table<T>> = LambdaWeakJoin(weakJoinFun)
    return WeakJoinHandle.of(
        lambda,
        false,
        false,
        weakJoin as WeakJoin<TableLike<*>, TableLike<*>>
    )
}

private class LambdaWeakJoin<S: Any, T: Any>(
    private val weakJoinFun: KWeakJoinFun<S, T>
) : KWeakJoin<S, T>() {

    override fun on(
        source: KNonNullTable<S>,
        target: KNonNullTable<T>,
        ctx: Context<S, T>
    ): KNonNullExpression<Boolean> {
        val funCtx = KWeakJoinFunContextImpl(source, target, ctx)
        return weakJoinFun.run {
            funCtx.on()
        }
    }
}
