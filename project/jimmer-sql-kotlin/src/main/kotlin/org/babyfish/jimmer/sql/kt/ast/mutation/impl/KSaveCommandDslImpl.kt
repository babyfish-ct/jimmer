package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.TypedProp
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.LockMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandDsl
import kotlin.reflect.KProperty1

internal class KSaveCommandDslImpl(
    private val javaCfg: AbstractEntitySaveCommand.Cfg
): KSaveCommandDsl {

    override fun setMode(mode: SaveMode) {
        javaCfg.setMode(mode)
    }

    override fun setAssociatedModeAll(mode: AssociatedSaveMode) {
        javaCfg.setAssociatedModeAll(mode)
    }

    override fun setAssociatedMode(prop: KProperty1<*, *>, mode: AssociatedSaveMode) {
        javaCfg.setAssociatedMode(prop.toImmutableProp(), mode)
    }

    override fun setAssociatedMode(prop: ImmutableProp, mode: AssociatedSaveMode) {
        javaCfg.setAssociatedMode(prop, mode)
    }

    override fun setAssociatedMode(prop: TypedProp.Association<*, *>, mode: AssociatedSaveMode) {
        javaCfg.setAssociatedMode(prop, mode)
    }

    override fun <E : Any> setKeyProps(vararg keyProps: KProperty1<E, *>) {
        javaCfg.setKeyProps(
            *keyProps.map { it.toImmutableProp() }.toTypedArray()
        )
    }

    override fun <E : Any> setKeyProps(vararg keyProps: TypedProp<E, *>) {
        javaCfg.setKeyProps(*keyProps)
    }

    override fun setAutoIdOnlyTargetCheckingAll() {
        javaCfg.setAutoIdOnlyTargetCheckingAll()
    }

    override fun setAutoIdOnlyTargetChecking(prop: KProperty1<*, *>) {
        javaCfg.setAutoIdOnlyTargetChecking(prop.toImmutableProp())
    }

    override fun setDissociateAction(prop: KProperty1<*, *>, action: DissociateAction) {
        javaCfg.setDissociateAction(prop.toImmutableProp(), action)
    }

    override fun setLockMode(lockMode: LockMode) {
        javaCfg.setLockMode(lockMode)
    }
}

