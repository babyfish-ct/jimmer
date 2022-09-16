package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand
import org.babyfish.jimmer.sql.kt.ast.mutation.KDeleteCommandDsl
import kotlin.reflect.KProperty1

internal class KDeleteCommandDslImpl(
    private val javaCfg: DeleteCommand.Cfg
): KDeleteCommandDsl {

    override fun setDissociateAction(prop: KProperty1<*, *>, action: DissociateAction) {
        javaCfg.setDissociateAction(prop.toImmutableProp(), action)
    }
}