package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;

public interface SimpleEntitySaveCommand<E>
        extends Executable<SimpleSaveResult<E>>,
        AbstractEntitySaveCommand {

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setMode(SaveMode mode);

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setAssociatedModeAll(AssociatedSaveMode mode);

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setAssociatedMode(ImmutableProp prop, AssociatedSaveMode mode);

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setAssociatedMode(TypedProp.Association<?, ?> prop, AssociatedSaveMode mode) {
        return setAssociatedMode(prop.unwrap(), mode);
    }

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setKeyProps(ImmutableProp... props) {
        return setKeyProps("", props);
    }

    @NewChain
    SimpleEntitySaveCommand<E> setKeyProps(String group, ImmutableProp... props);

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setKeyProps(TypedProp.Single<?, ?>... props) {
        return setKeyProps("", props);
    }

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setKeyProps(String group, TypedProp.Single<?, ?>... props) {
        ImmutableProp[] unwrappedProps = new ImmutableProp[props.length];
        for (int i = 0; i < props.length; i++) {
            unwrappedProps[i] = props[i].unwrap();
        }
        return setKeyProps(group, unwrappedProps);
    }

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setUpsertMask(ImmutableProp... props) {
        if (props.length == 0) {
            throw new IllegalArgumentException("props cannot be empty");
        }
        UpsertMask<?> mask = UpsertMask.of(props[0].getDeclaringType().getJavaClass());
        for (ImmutableProp prop : props) {
            mask = mask.addUpdatableProp(prop);
        }
        return setUpsertMask(mask);
    }

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setUpsertMask(TypedProp.Single<?, ?>... props) {
        if (props.length == 0) {
            throw new IllegalArgumentException("props cannot be empty");
        }
        UpsertMask<?> mask = UpsertMask.of(props[0].unwrap().getDeclaringType().getJavaClass());
        for (TypedProp.Single<?, ?> prop : props) {
            mask = mask.addUpdatableProp(prop.unwrap());
        }
        return setUpsertMask(mask);
    }

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setUpsertMask(UpsertMask<?> mask);

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setAutoIdOnlyTargetCheckingAll();

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop) {
        return setAutoIdOnlyTargetChecking(prop.unwrap());
    }

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop, boolean checking) {
        return setAutoIdOnlyTargetChecking(prop.unwrap(), checking);
    }

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop) {
        return setAutoIdOnlyTargetChecking(prop, true);
    }

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop, boolean checking);

    @Override
    default SimpleEntitySaveCommand<E> setIdOnlyAsReference(TypedProp.Association<?, ?> prop, boolean asReference) {
        return setIdOnlyAsReference(prop.unwrap(), asReference);
    }

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setIdOnlyAsReferenceAll(boolean asReference);

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setIdOnlyAsReference(ImmutableProp prop, boolean asReference);

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setKeyOnlyAsReferenceAll();

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setKeyOnlyAsReference(TypedProp.Association<?, ?> prop) {
        return setKeyOnlyAsReference(prop.unwrap(), true);
    }

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setKeyOnlyAsReference(TypedProp.Association<?, ?> prop, boolean asReference) {
        return setKeyOnlyAsReference(prop.unwrap(), asReference);
    }

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setKeyOnlyAsReference(ImmutableProp prop) {
        return setKeyOnlyAsReference(prop, true);
    }

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setKeyOnlyAsReference(ImmutableProp prop, boolean asReference);

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setDissociateAction(
            TypedProp.Reference<?, ?> prop,
            DissociateAction dissociateAction
    ) {
        return setDissociateAction(prop.unwrap(), dissociateAction);
    }

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setDissociateAction(
            ImmutableProp prop,
            DissociateAction dissociateAction
    );

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setTargetTransferMode(
            TypedProp.ReferenceList<?, ?> prop,
            TargetTransferMode mode
    ) {
        return setTargetTransferMode(prop.unwrap(), mode);
    }

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setTargetTransferMode(
            ImmutableProp prop,
            TargetTransferMode mode
    );

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setTargetTransferModeAll(TargetTransferMode mode);

    @NewChain
    SimpleEntitySaveCommand<E> setPessimisticLock(Class<?> entityType, boolean lock);

    @NewChain
    default SimpleEntitySaveCommand<E> setPessimisticLock(Class<?> entityType) {
        return setPessimisticLock(entityType, true);
    }

    @NewChain
    SimpleEntitySaveCommand<E> setPessimisticLockAll();

    /**
     * Example: <pre>{@code
     *  sqlClient
     *      .getEntities()
     *      .saveCommand(process)
     *      .setOptimisticLock(ProcessTable.class, (table, vf) -> {
     *          return Predicate.and(
     *              table.version().eq(vf.newNumber(ProcessProps.VERSION)),
     *              table.status().eq(States.PENDING)
     *          );
     *      })
     *      .execute()
     * }</pre>
     */
    @NewChain
    default <T extends Table<E>> SimpleEntitySaveCommand<E> setOptimisticLock(
            Class<T> tableType,
            UserOptimisticLock<E, T> block
    ) {
        return setOptimisticLock(tableType, UnloadedVersionBehavior.IGNORE, block);
    }

    /**
     * Example: <pre>{@code
     *  sqlClient
     *      .getEntities()
     *      .saveCommand(process)
     *      .setOptimisticLock(ProcessTable.class, UnloadedVersionBehavior.INCREASE, (table, vf) -> {
     *          return Predicate.and(
     *              table.version().eq(vf.newNumber(ProcessProps.VERSION)),
     *              table.status().eq(States.PENDING)
     *          );
     *      })
     *      .execute()
     * }</pre>
     */
    @NewChain
    <T extends Table<E>> SimpleEntitySaveCommand<E> setOptimisticLock(
            Class<T> tableType,
            UnloadedVersionBehavior behavior,
            UserOptimisticLock<E, T> block
    );

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setDeleteMode(DeleteMode mode);

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setMaxCommandJoinCount(int count);

    @Override
    default SimpleEntitySaveCommand<E> setDumbBatchAcceptable() {
        return setDumbBatchAcceptable(true);
    }

    @Override
    SimpleEntitySaveCommand<E> setDumbBatchAcceptable(boolean acceptable);

    @Override
    SimpleEntitySaveCommand<E> setConstraintViolationTranslatable(boolean transferable);

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> addExceptionTranslator(ExceptionTranslator<?> translator);

    @Override
    SimpleEntitySaveCommand<E> setTransactionRequired(boolean required);
}
