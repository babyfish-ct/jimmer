package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.DeleteAction
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand
import org.babyfish.jimmer.sql.kt.ast.mutation.KDeleteCommandDsl
import org.babyfish.jimmer.sql.kt.toImmutableProp
import kotlin.reflect.KProperty1

internal class KDeleteCommandDslImpl(
    private val javaCfg: DeleteCommand.Cfg
): KDeleteCommandDsl {

    override fun setDeleteAction(prop: KProperty1<*, *>, action: DeleteAction) {
        javaCfg.setDeleteAction(prop.toImmutableProp(), action)
    }
}