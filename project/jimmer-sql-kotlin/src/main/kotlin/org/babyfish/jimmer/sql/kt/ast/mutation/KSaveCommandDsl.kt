package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import kotlin.reflect.KProperty1

@DslScope
interface KSaveCommandDsl {

    fun setMode(mode: SaveMode)

    fun <E: Any> setKeyProps(vararg keyProps: KProperty1<E, *>)

    fun setAutoAttachingAll()

    fun setAutoAttaching(prop: KProperty1<*, *>)

    fun setAutoIdOnlyTargetChecking(prop: KProperty1<*, *>)

    fun setAutoIdOnlyTargetCheckingAll()

    fun setAppendOnlyAll()

    fun setAppendOnly(prop: KProperty1<*, *>)

    fun setDissociateAction(prop: KProperty1<*, *>, action: DissociateAction)
}