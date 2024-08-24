package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.TypedProp
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.LockMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.TargetTransferMode
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@DslScope
interface KSaveCommandDsl {

    fun setMode(mode: SaveMode)

    fun setAssociatedModeAll(mode: AssociatedSaveMode)

    fun setAssociatedMode(prop: KProperty1<*, *>, mode: AssociatedSaveMode)

    fun setAssociatedMode(prop: ImmutableProp, mode: AssociatedSaveMode)

    fun setAssociatedMode(prop: TypedProp.Association<*, *>, mode: AssociatedSaveMode)

    fun <E: Any> setKeyProps(vararg keyProps: KProperty1<E, *>)

    fun <E: Any> setKeyProps(vararg keyProps: TypedProp<E, *>)

    fun <E: Any> setOptimisticLock(
        type: KClass<E>,
        block: (OptimisticLockContext<E>).() -> KNonNullExpression<Boolean>?
    )

    fun setAutoIdOnlyTargetCheckingAll()

    fun setAutoIdOnlyTargetChecking(prop: KProperty1<*, *>)

    fun setDissociateAction(prop: KProperty1<*, *>, action: DissociateAction)

    fun setLockMode(lockMode: LockMode)

    fun setTargetTransferMode(prop: KProperty1<*, *>, mode: TargetTransferMode)

    fun setTargetTransferModeAll(mode: TargetTransferMode)

    data class OptimisticLockContext<E: Any>(
        val table: KNonNullTable<E>,
        val entity: E
    )
}