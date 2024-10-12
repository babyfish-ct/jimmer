package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode

@DslScope
interface KSaveCommandDsl : KSaveCommandPartialDsl {

    fun setMode(mode: SaveMode)

    fun setAssociatedModeAll(mode: AssociatedSaveMode)
}