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

    @NewChain
    AbstractEntitySaveCommand setUpsertMask(ImmutableProp ... props);

    @NewChain
    AbstractEntitySaveCommand setUpsertMask(TypedProp.Single<?, ?> ... props);

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
