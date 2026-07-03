package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.TypeMatchMode
import kotlin.reflect.KProperty1

@DslScope
interface KDeleteCommandDsl {

    fun setMode(mode: DeleteMode)

    fun setTypeMatchMode(mode: TypeMatchMode)

    fun setDissociateAction(prop: KProperty1<*, *>, action: DissociateAction)

    fun setDumbBatchAcceptable(acceptable: Boolean)

    fun setTransactionRequired(required: Boolean)
}
