package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.DissociateAction
import kotlin.reflect.KProperty1

@DslScope
interface KDeleteCommandDsl {

    fun setDissociateAction(prop: KProperty1<*, *>, action: DissociateAction)
}