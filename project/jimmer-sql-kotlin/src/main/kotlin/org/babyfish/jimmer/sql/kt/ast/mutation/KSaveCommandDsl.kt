package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import kotlin.reflect.KProperty1

@DslScope
interface KSaveCommandDsl {

    fun setMode(mode: SaveMode)

    fun <E: Any> setKeyProps(vararg keyProps: KProperty1<E, *>)

    @Deprecated("Will be deleted in 1.0")
    fun setAutoAttachingAll()

    @Deprecated("Will be deleted in 1.0")
    fun setAutoAttaching(prop: KProperty1<*, *>)

    fun setAutoIdOnlyTargetChecking(prop: KProperty1<*, *>)

    fun setAutoIdOnlyTargetCheckingAll()

    /**
     * Enable/Disable the merge mode.
     *
     * <p>
     *     If the merge mode is enabled, for each object of the save tree,
     *     only insert and update operations will be executed,
     *     dissociation operations will never be executed.
     * </p>
     * @param mergeMode is merge mode enable
     */
    fun setMergeMode(mergeMode: Boolean = true)

    @Deprecated("Will be deleted in 1.0", replaceWith = ReplaceWith("setMergeMode"))
    fun setAppendOnlyAll()

    @Deprecated("Will be deleted in 1.0", replaceWith = ReplaceWith("setMergeMode"))
    fun setAppendOnly(prop: KProperty1<*, *>)

    fun setDissociateAction(prop: KProperty1<*, *>, action: DissociateAction)

    fun setPessimisticLock(pessimisticLock: Boolean = true)
}