package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.ast.impl.table.AbstractWeakJoinLambdaFactory
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinLambda
import org.babyfish.jimmer.sql.kt.ast.table.KPropsWeakJoinFun

internal class KPropsWeakJoinLambdaFactory(
    private val sourceType: Class<*>,
    private val targetType: Class<*>
) : AbstractWeakJoinLambdaFactory() {

    fun get(fn: KPropsWeakJoinFun<*, *>): WeakJoinLambda? =
        getLambda(fn)

    override fun getTypes(implClass: String, implMethodSignature: String): Array<Class<*>> {
        return arrayOf(sourceType, targetType)
    }
}
