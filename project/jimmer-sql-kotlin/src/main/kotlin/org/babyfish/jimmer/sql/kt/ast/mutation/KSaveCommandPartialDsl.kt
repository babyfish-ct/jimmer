package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.TypedProp
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.TargetTransferMode
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.mutation.UnloadedVersionBehavior
import org.babyfish.jimmer.sql.ast.mutation.UpsertMask
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KProps
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@DslScope
interface KSaveCommandPartialDsl {

    fun setAssociatedMode(prop: KProperty1<*, *>, mode: AssociatedSaveMode)

    fun setAssociatedMode(prop: ImmutableProp, mode: AssociatedSaveMode)

    fun setAssociatedMode(prop: TypedProp.Association<*, *>, mode: AssociatedSaveMode)

    fun <E: Any> setKeyProps(vararg keyProps: KProperty1<E, *>)

    fun <E: Any> setKeyProps(vararg keyProps: TypedProp.Single<E, *>)

    fun <E: Any> setKeyProps(group: String, vararg keyProps: KProperty1<E, *>)

    fun <E: Any> setKeyProps(group: String, vararg keyProps: TypedProp.Single<E, *>)

    /**
     * Set UpsertMask with updatable properties
     *
     * When upsert is executed, existing rows will be updated.
     * By default, the properties is determined by object shape
     *
     * ```
     * updatedProperties = propertiesOf(dynamicEntity) - conflictIdOrKey
     * ```
     *
     * If the UpsertMask is specified,
     *
     * ```
     * updatedProperties = (
     *      propertiesOf(dynamicEntity) - conflictIdOrKey
     * ) & upsertMask.updatableProps
     * ```
     *
     * @param props Properties that can be updated
     *
     *          - its length cannot be 0
     *          - all properties must belong to one entity type
     */
    fun <E: Any> setUpsertMask(vararg props: ImmutableProp)

    /**
     * Set UpsertMask with updatable properties
     *
     * When upsert is executed, existing rows will be updated.
     * By default, the properties is determined by object shape
     *
     * ```
     * updatedProperties = propertiesOf(dynamicEntity) - conflictIdOrKey
     * ```
     *
     * If the UpsertMask is specified,
     *
     * ```
     * updatedProperties = (
     *      propertiesOf(dynamicEntity) - conflictIdOrKey
     * ) & upsertMask.updatableProps
     * ```
     *
     * @param props Properties that can be updated
     *
     *          - its length cannot be 0
     *          - all properties must belong to one entity type
     */
    fun <E: Any> setUpsertMask(vararg props: KProperty1<E, *>)

    /**
     * Set UpsertMask with updatable properties
     *
     * When upsert is executed, existing rows will be updated.
     * By default, the properties is determined by object shape
     *
     * ```
     * updatedProperties = propertiesOf(dynamicEntity) - conflictIdOrKey
     * ```
     *
     * If the UpsertMask is specified,
     *
     * ```
     * updatedProperties = (
     *      propertiesOf(dynamicEntity) - conflictIdOrKey
     * ) & upsertMask.updatableProps
     * ```
     *
     * @param props Properties that can be updated
     *
     *          - its length cannot be 0
     *          - all properties must belong to one entity type
     */
    fun <E: Any> setUpsertMask(vararg props: TypedProp.Single<E, *>)

    /**
     * Set UpsertMask object
     *
     * When upsert is executed, existing rows will be updated
     * and non-existing rows will be inserted.
     * By default, the properties is determined by object shape
     *
     * ```
     * insertedProperties = propertiesOf(dynamicEntity)
     * updatedProperties = propertiesOf(dynamicEntity) - conflictIdOrKey
     * ```
     *
     * If the UpsertMask is specified,
     *
     * ```
     * insertedProperties = (
     *      propertiesOf(dynamicEntity) & upsertMask.insertableProps
     * ) + conflictIdOrKey
     * updatedProperties = (
     *      propertiesOf(dynamicEntity) - conflictIdOrKey
     * ) & upsertMask.updatableProps
     * ```
     *
     * @param mask The upsert mask object, it cannot be null
     */
    fun setUpsertMask(mask: UpsertMask<*>)

    /**
     * Example:
     * ```
     * sqlClient.save(process) {
     *      setOptimisticLock(Process::class) {
     *          and(
     *              table.version eq newNonNull(Process::version),
     *              table.status eq Status.PENDING
     *          )
     *      }
     * }
     * ```
     */
    fun <E: Any> setOptimisticLock(
        type: KClass<E>,
        behavior: UnloadedVersionBehavior = UnloadedVersionBehavior.IGNORE,
        block: (OptimisticLockContext<E>).() -> KNonNullExpression<Boolean>?
    )

    fun <E: Any> setPessimisticLock(entityType: KClass<E>, lock: Boolean = true)

    fun setPessimisticLockAll()

    fun setAutoIdOnlyTargetCheckingAll()

    fun setAutoIdOnlyTargetChecking(prop: KProperty1<*, *>)

    fun setKeyOnlyAsReferenceAll()

    fun setKeyOnlyAsReference(prop: KProperty1<*, *>)

    fun setDissociateAction(prop: KProperty1<*, *>, action: DissociateAction)

    fun setTargetTransferMode(prop: KProperty1<*, *>, mode: TargetTransferMode)

    fun setTargetTransferModeAll(mode: TargetTransferMode)

    fun setDumbBatchAcceptable(acceptable: Boolean = true)

    fun addExceptionTranslator(translator: ExceptionTranslator<*>?)

    fun setDeleteMode(mode: DeleteMode)

    fun setMaxCommandJoinCount(count: Int)

    interface OptimisticLockContext<E: Any> {
        val table: KNonNullTable<E>
        fun <V: Any> newNonNull(prop: KProperty1<E, V>): KNonNullExpression<V>
        fun <V: Any> newNullable(prop: KProperty1<E, V?>): KNullableExpression<V>
    }
}