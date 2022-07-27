package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.DeleteAction
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand
import org.babyfish.jimmer.sql.kt.ast.mutation.KDeleteCommandDsl
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaMethod

internal class KDeleteCommandDslImpl(
    private val javaCfg: DeleteCommand.Cfg
): KDeleteCommandDsl {

    override fun setDeleteAction(prop: KProperty1<*, *>, action: DeleteAction) {
        javaCfg.setDeleteAction(
            ImmutableType
                .get(prop.getter.javaMethod!!.declaringClass)
                .getProp(prop.name),
            action
        )
    }
}