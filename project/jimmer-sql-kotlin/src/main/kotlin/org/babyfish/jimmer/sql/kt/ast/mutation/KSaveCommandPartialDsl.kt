package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.TypedProp
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.TargetTransferMode
import org.babyfish.jimmer.sql.ast.TypeMatchMode
import org.babyfish.jimmer.sql.ast.mutation.*
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@DslScope
interface KSaveCommandPartialDsl {

    /**
     * Configure how the `@Version` property of the root entity is saved.
     * This configuration does not propagate to associated entities.
     */
    fun setVersionMode(mode: VersionMode)

    fun setAssociatedMode(prop: KProperty1<*, *>, mode: AssociatedSaveMode)

    fun setAssociatedMode(prop: ImmutableProp, mode: AssociatedSaveMode)

    fun setAssociatedMode(prop: TypedProp.Association<*, *>, mode: AssociatedSaveMode)

    /**
     * Matches root entities by the single key group declared in their model, even when an id is supplied.
     * A supplied id is used only for inserting a new row. Matching an existing row never replaces its id;
     * accepted results contain that existing id.
     *
     * Explicit `setKeyProps` for the root type takes precedence, regardless of call order.
     * Otherwise, exactly one named or unnamed model key group must exist. Missing or ambiguous groups
     * cause an error when the command is executed. A complete key must be loaded; there is no fallback to id.
     * Applies to the root types throughout the saved graph. Other types retain their matching rules.
     * [SaveMode.INSERT_ONLY] performs no conflict lookup and does not require loaded keys.
     */
    fun matchByKey()

    /**
     * Configures the unnamed key group for the properties' declaring entity type.
     * This defines the key properties without changing the matching priority: a supplied id still wins.
     * Objects without an id can be matched by a loaded key group.
     * Does not enable [matchByKey] automatically.
     * To match root entities by key even when an id is supplied, also call [matchByKey].
     *
     * Applies to this type throughout the saved graph. Other types keep their usual id-first matching.
     * The properties must belong to one type and form a unique key in the database.
     */
    fun <E : Any> setKeyProps(vararg keyProps: KProperty1<E, *>)

    /** Typed-property form of the unnamed key-group configuration; does not enable [matchByKey]. */
    fun <E : Any> setKeyProps(vararg keyProps: TypedProp.Single<E, *>)

    /**
     * Configures [group] without changing id precedence; does not enable [matchByKey].
     * Replaces the group of the same name and preserves other key groups.
     */
    fun <E : Any> setKeyProps(group: String, vararg keyProps: KProperty1<E, *>)

    /** Typed-property form of the named key-group configuration; does not enable [matchByKey]. */
    fun <E : Any> setKeyProps(group: String, vararg keyProps: TypedProp.Single<E, *>)

    /**
     * Forbid update assignments derived from entity properties during upsert.
     *
     * Equivalent to an [UpsertMask] with no updatable properties. A dialect can still render
     * a technical update such as `SET column = column` to resolve a conflict and return the
     * existing row's id or other result fields in one atomic statement. This includes ids
     * needed to save associations, even without an explicit returning request. Such an SQL
     * update can contribute to affected-row counts and invoke database update triggers.
     *
     * This preserves [SaveMode.UPSERT] semantics: an existing row can be accepted and returned
     * even though no property values are assigned from the input. Use [SaveMode.INSERT_IF_ABSENT]
     * to skip conflicting rows instead; those rows are not accepted and must be queried
     * separately if their data is needed.
     */
    fun forbidUpdate()

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
     *          - An empty array selects no entity properties for update
     *          - all properties must belong to one entity type
     */
    fun <E : Any> setUpsertMask(vararg props: ImmutableProp)

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
     *          - An empty array selects no entity properties for update
     *          - all properties must belong to one entity type
     */
    fun <E : Any> setUpsertMask(vararg props: KProperty1<E, *>)

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
     *          - An empty array selects no entity properties for update
     *          - all properties must belong to one entity type
     */
    fun <E : Any> setUpsertMask(vararg props: TypedProp.Single<E, *>)

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

    fun <E : Any, V : Any> set(
        prop: KProperty1<E, V>,
        block: AssignmentContext<E>.() -> KNonNullExpression<V>
    )

    fun <E : Any, V : Any> setNullable(
        prop: KProperty1<E, V?>,
        block: AssignmentContext<E>.() -> KExpression<V>
    )

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
    fun <E : Any> setOptimisticLock(
        type: KClass<E>,
        behavior: UnloadedVersionBehavior = UnloadedVersionBehavior.IGNORE,
        block: UpdateConditionContext<E>.() -> KNonNullExpression<Boolean>?
    )

    fun <E : Any> setUpdateWhere(
        type: KClass<E>,
        block: UpdateConditionContext<E>.() -> KNonNullExpression<Boolean>?
    )

    fun <E : Any> setPessimisticLock(entityType: KClass<E>, lock: Boolean = true)

    fun setPessimisticLockAll()

    fun setAutoIdOnlyTargetCheckingAll()

    fun setAutoIdOnlyTargetChecking(prop: KProperty1<*, *>)

    fun setIdOnlyAsReferenceAll(asReference: Boolean)

    fun setIdOnlyAsReference(prop: KProperty1<*, *>, asReference: Boolean)

    fun setKeyOnlyAsReferenceAll()

    fun setKeyOnlyAsReference(prop: KProperty1<*, *>, asReference: Boolean = true)

    fun setDissociateAction(prop: KProperty1<*, *>, action: DissociateAction)

    fun setTargetTransferMode(prop: KProperty1<*, *>, mode: TargetTransferMode)

    fun setTargetTransferModeAll(mode: TargetTransferMode)

    fun setTypeMatchMode(mode: TypeMatchMode)

    fun setAssociatedTypeMatchModeAll(mode: TypeMatchMode)

    fun <E : Any> setAssociatedTypeMatchMode(entityType: KClass<E>, mode: TypeMatchMode)

    fun setAssociatedTypeMatchMode(prop: KProperty1<*, *>, mode: TypeMatchMode)

    fun setAssociatedTypeMatchMode(prop: ImmutableProp, mode: TypeMatchMode)

    fun setAssociatedTypeMatchMode(prop: TypedProp.Association<*, *>, mode: TypeMatchMode)

    fun setTypeChangeAllowed(allowed: Boolean = true)

    fun setAssociatedTypeChangeAllowedAll(allowed: Boolean = true)

    fun <E : Any> setAssociatedTypeChangeAllowed(entityType: KClass<E>, allowed: Boolean = true)

    fun setAssociatedTypeChangeAllowed(prop: KProperty1<*, *>, allowed: Boolean = true)

    fun setAssociatedTypeChangeAllowed(prop: ImmutableProp, allowed: Boolean = true)

    fun setAssociatedTypeChangeAllowed(prop: TypedProp.Association<*, *>, allowed: Boolean = true)

    fun setDumbBatchAcceptable(acceptable: Boolean = true)

    fun setSaveReturningEnabled(enabled: Boolean = true)

    fun setSaveResultReadsAllProperties(readsAllProperties: Boolean = true)

    fun setConstraintViolationTranslatable(translatable: Boolean = true)

    fun addExceptionTranslator(translator: ExceptionTranslator<*>?)

    fun setDeleteMode(mode: DeleteMode)

    fun setMaxCommandJoinCount(count: Int)

    fun setTransactionRequired(required: Boolean)

    fun setEnabled(enabled: Boolean)

    interface ValueExpressionContext<E : Any> {
        fun <V : Any> newNonNull(prop: KProperty1<E, V>): KNonNullExpression<V>
        fun <V : Any> newNullable(prop: KProperty1<E, V?>): KNullableExpression<V>
    }

    interface AssignmentContext<E : Any> : ValueExpressionContext<E> {
        val target: KNonNullTable<E>
    }

    interface UpdateConditionContext<E : Any> : ValueExpressionContext<E> {
        val table: KNonNullTable<E>
    }
}
