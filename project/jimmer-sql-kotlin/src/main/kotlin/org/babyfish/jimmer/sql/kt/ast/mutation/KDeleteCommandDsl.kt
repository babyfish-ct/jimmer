package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.DeleteAction
import kotlin.reflect.KProperty1

@DslScope
interface KDeleteCommandDsl {

    fun setDeleteAction(prop: KProperty1<*, *>, action: DeleteAction)
}