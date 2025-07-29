package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.ast.impl.table.AbstractWeakJoinLambdaFactory
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinLambda
import org.babyfish.jimmer.sql.kt.ast.table.KPropsWeakJoinFun
import java.lang.invoke.SerializedLambda

internal class KPropsWeakJoinLambdaFactory(
    private val sourceType: Class<*>,
    private val targetType: Class<*>
) : AbstractWeakJoinLambdaFactory() {

    fun get(fn: KPropsWeakJoinFun<*, *>): WeakJoinLambda? =
        getLambda(fn)

    override fun getTypes(serializedLambda: SerializedLambda?): Array<Class<*>?> {
        return arrayOf(sourceType, targetType)
    }
}