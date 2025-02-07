package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.ast.mutation.DeleteCommand
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.kt.ast.mutation.KDeleteCommandDsl
import kotlin.reflect.KProperty1

internal class KDeleteCommandDslImpl(
    internal var javaCommand: DeleteCommand
): KDeleteCommandDsl {

    override fun setMode(mode: DeleteMode) {
        javaCommand = javaCommand.setMode(mode)
    }

    override fun setDissociateAction(prop: KProperty1<*, *>, action: DissociateAction) {
        javaCommand = javaCommand.setDissociateAction(prop.toImmutableProp(), action)
    }

    override fun setDumbBatchAcceptable(acceptable: Boolean) {
        javaCommand = javaCommand.setDumbBatchAcceptable(acceptable)
    }

    override fun setTransactionRequired(required: Boolean) {
        javaCommand = javaCommand.setTransactionRequired(required)
    }
}