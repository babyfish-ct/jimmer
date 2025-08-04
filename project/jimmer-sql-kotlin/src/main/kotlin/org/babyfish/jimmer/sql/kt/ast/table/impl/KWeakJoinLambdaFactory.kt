package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.sql.ast.impl.table.AbstractWeakJoinLambdaFactory
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinLambda
import org.babyfish.jimmer.sql.kt.ast.table.KPropsWeakJoinFun
import org.babyfish.jimmer.sql.kt.ast.table.KWeakJoinFun
import java.lang.invoke.SerializedLambda

internal class KWeakJoinLambdaFactory(
    private val sourceEntityType: Class<*>,
    private val targetEntityType: Class<*>
) : AbstractWeakJoinLambdaFactory() {

    fun get(fn: KWeakJoinFun<*, *>): WeakJoinLambda? =
        getLambda(fn)

    override fun getTypes(serializedLambda: SerializedLambda?): Array<Class<*>> {
        return arrayOf(sourceEntityType, targetEntityType)
    }
}