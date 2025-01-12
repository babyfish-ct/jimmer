package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;

public interface AbstractEntitySaveCommand {

    @NewChain
    AbstractEntitySaveCommand setMode(SaveMode mode);

    @NewChain
    AbstractEntitySaveCommand setAssociatedModeAll(AssociatedSaveMode mode);

    @NewChain
    AbstractEntitySaveCommand setAssociatedMode(ImmutableProp prop, AssociatedSaveMode mode);

    @NewChain
    AbstractEntitySaveCommand setAssociatedMode(TypedProp.Association<?, ?> prop, AssociatedSaveMode mode);

    @NewChain
    AbstractEntitySaveCommand setKeyProps(ImmutableProp ... props);

    @NewChain
    AbstractEntitySaveCommand setKeyProps(String group, ImmutableProp ... props);

    @NewChain
    AbstractEntitySaveCommand setKeyProps(TypedProp.Single<?, ?> ... props);

    @NewChain
    AbstractEntitySaveCommand setKeyProps(String group, TypedProp.Single<?, ?> ... props);

    /**
     * Set UpsertMask with updatable properties
     *
     * <p>When upsert is executed, existing rows will be updated.
     * By default, the properties is determined by object shape</p>
     *
     * <pre>{@code
     * updatedProperties = propertiesOf(dynamicEntity) - conflictIdOrKey
     * }</pre>
     *
     * <p>If the UpsertMask is specified,</p>
     *
     * <pre>{@code
     * updatedProperties = (
     *      propertiesOf(dynamicEntity) - conflictIdOrKey
     * ) & upsertMask.updatableProps
     * }</pre>
     *
     * @param props Properties that can be updated
     *          <ul>
     *              <li>its length cannot be 0</li>
     *              <li>all properties must belong to one entity type</li>
     *          </ul>
     */
    @NewChain
    AbstractEntitySaveCommand setUpsertMask(ImmutableProp ... props);

    /**
     * Set UpsertMask with updatable properties
     *
     * <p>When upsert is executed, existing rows will be updated.
     * By default, the properties is determined by object shape</p>
     *
     * <pre>{@code
     * updatedProperties = propertiesOf(dynamicEntity) - conflictIdOrKey
     * }</pre>
     *
     * <p>If the UpsertMask is specified,</p>
     *
     * <pre>{@code
     * updatedProperties = (
     *      propertiesOf(dynamicEntity) - conflictIdOrKey
     * ) & upsertMask.updatableProps
     * }</pre>
     *
     * @param props Properties that can be updated
     *          <ul>
     *              <li>its length cannot be 0</li>
     *              <li>all properties must belong to one entity type</li>
     *          </ul>
     */
    @NewChain
    AbstractEntitySaveCommand setUpsertMask(TypedProp.Single<?, ?> ... props);

    /**
     * Set UpsertMask object
     *
     * <p>When upsert is executed, existing rows will be updated
     * and non-existing rows will be inserted.
     * By default, the properties is determined by object shape</p>
     *
     * <pre>{@code
     * insertedProperties = propertiesOf(dynamicEntity)
     * updatedProperties = propertiesOf(dynamicEntity) - conflictIdOrKey
     * }</pre>
     *
     * <p>If the UpsertMask is specified,</p>
     *
     * <pre>{@code
     * insertedProperties = (
     *      propertiesOf(dynamicEntity) & upsertMask.insertableProps
     * ) + conflictIdOrKey
     * updatedProperties = (
     *      propertiesOf(dynamicEntity) - conflictIdOrKey
     * ) & upsertMask.updatableProps
     * }</pre>
     *
     * @param mask The upsert mask object, it cannot be null
     */
    @NewChain
    AbstractEntitySaveCommand setUpsertMask(UpsertMask<?> mask);

    @NewChain
    AbstractEntitySaveCommand setAutoIdOnlyTargetCheckingAll();

    @NewChain
    AbstractEntitySaveCommand setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop);

    @NewChain
    AbstractEntitySaveCommand setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop, boolean checking);

    @NewChain
    AbstractEntitySaveCommand setAutoIdOnlyTargetChecking(ImmutableProp prop);

    @NewChain
    AbstractEntitySaveCommand setAutoIdOnlyTargetChecking(ImmutableProp prop, boolean checking);

    @NewChain
    AbstractEntitySaveCommand setKeyOnlyAsReferenceAll();

    @NewChain
    AbstractEntitySaveCommand setKeyOnlyAsReference(ImmutableProp prop);

    @NewChain
    AbstractEntitySaveCommand setKeyOnlyAsReference(ImmutableProp prop, boolean asReference);

    @NewChain
    AbstractEntitySaveCommand setKeyOnlyAsReference(TypedProp.Association<?, ?> prop);

    @NewChain
    AbstractEntitySaveCommand setKeyOnlyAsReference(TypedProp.Association<?, ?> prop, boolean asReference);

    @NewChain
    default AbstractEntitySaveCommand setDissociateAction(
            TypedProp.Reference<?, ?> prop,
            DissociateAction dissociateAction
    ) {
        return setDissociateAction(prop.unwrap(), dissociateAction);
    }

    @NewChain
    AbstractEntitySaveCommand setDissociateAction(
            ImmutableProp prop,
            DissociateAction dissociateAction
    );

    @NewChain
    default AbstractEntitySaveCommand setTargetTransferMode(
            TypedProp.ReferenceList<?, ?> prop,
            TargetTransferMode mode
    ) {
        return setTargetTransferMode(prop.unwrap(), mode);
    }

    @NewChain
    AbstractEntitySaveCommand setTargetTransferMode(
            ImmutableProp prop,
            TargetTransferMode mode
    );

    @NewChain
    AbstractEntitySaveCommand setTargetTransferModeAll(TargetTransferMode mode);

    @NewChain
    AbstractEntitySaveCommand setDeleteMode(DeleteMode mode);

    @NewChain
    AbstractEntitySaveCommand setMaxCommandJoinCount(int count);

    @NewChain
    AbstractEntitySaveCommand setPessimisticLock(Class<?> entityType, boolean lock);

    @NewChain
    AbstractEntitySaveCommand setPessimisticLock(Class<?> entityType);

    @NewChain
    AbstractEntitySaveCommand setPessimisticLockAll();

    @NewChain
    AbstractEntitySaveCommand setDumbBatchAcceptable();

    @NewChain
    AbstractEntitySaveCommand setDumbBatchAcceptable(boolean acceptable);

    @NewChain
    AbstractEntitySaveCommand addExceptionTranslator(ExceptionTranslator<?> translator);
}
