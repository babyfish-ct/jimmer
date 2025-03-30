package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;

import java.sql.Connection;

public interface BatchEntitySaveCommand<E>
        extends Executable<BatchSaveResult<E>>,
        AbstractEntitySaveCommand {

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setMode(SaveMode mode);

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setAssociatedModeAll(AssociatedSaveMode mode);

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setAssociatedMode(ImmutableProp prop, AssociatedSaveMode mode);

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setAssociatedMode(TypedProp.Association<?, ?> prop, AssociatedSaveMode mode) {
        return setAssociatedMode(prop.unwrap(), mode);
    }

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setKeyProps(ImmutableProp... props) {
        return setKeyProps("", props);
    }

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setKeyProps(String group, ImmutableProp... props);

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setKeyProps(TypedProp.Single<?, ?>... props) {
        return setKeyProps("", props);
    }

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setKeyProps(String group, TypedProp.Single<?, ?>... props) {
        ImmutableProp[] unwrappedProps = new ImmutableProp[props.length];
        for (int i = 0; i < props.length; i++) {
            unwrappedProps[i] = props[i].unwrap();
        }
        return setKeyProps(group, unwrappedProps);
    }

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setUpsertMask(ImmutableProp... props) {
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
    default BatchEntitySaveCommand<E> setUpsertMask(TypedProp.Single<?, ?>... props) {
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
    BatchEntitySaveCommand<E> setUpsertMask(UpsertMask<?> mask);

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setAutoIdOnlyTargetCheckingAll();

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop) {
        return setAutoIdOnlyTargetChecking(prop.unwrap());
    }

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop, boolean checking) {
        return setAutoIdOnlyTargetChecking(prop.unwrap(), checking);
    }

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop) {
        return setAutoIdOnlyTargetChecking(prop, true);
    }

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop, boolean checking);

    @Override
    BatchEntitySaveCommand<E> setIdOnlyAsReferenceAll(boolean asReference);

    @Override
    default BatchEntitySaveCommand<E> setIdOnlyAsReference(TypedProp.Association<?, ?> prop, boolean asReference) {
        return setIdOnlyAsReference(prop.unwrap(), asReference);
    }

    @Override
    BatchEntitySaveCommand<E> setIdOnlyAsReference(ImmutableProp prop, boolean asReference);

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setKeyOnlyAsReferenceAll();

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setKeyOnlyAsReference(TypedProp.Association<?, ?> prop) {
        return setKeyOnlyAsReference(prop.unwrap(), true);
    }

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setKeyOnlyAsReference(TypedProp.Association<?, ?> prop, boolean asReference) {
        return setKeyOnlyAsReference(prop.unwrap(), asReference);
    }

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setKeyOnlyAsReference(ImmutableProp prop) {
        return setKeyOnlyAsReference(prop, true);
    }

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setKeyOnlyAsReference(ImmutableProp prop, boolean asReference);

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setDissociateAction(
            TypedProp.Reference<?, ?> prop,
            DissociateAction dissociateAction
    ) {
        return setDissociateAction(prop.unwrap(), dissociateAction);
    }

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setDissociateAction(
            ImmutableProp prop,
            DissociateAction dissociateAction
    );

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setTargetTransferMode(
            TypedProp.ReferenceList<?, ?> prop,
            TargetTransferMode mode
    ) {
        return setTargetTransferMode(prop.unwrap(), mode);
    }

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setTargetTransferMode(
            ImmutableProp prop,
            TargetTransferMode mode
    );

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setTargetTransferModeAll(TargetTransferMode mode);

    @NewChain
    BatchEntitySaveCommand<E> setPessimisticLock(Class<?> entityType, boolean lock);

    @NewChain
    default BatchEntitySaveCommand<E> setPessimisticLock(Class<?> entityType) {
        return setPessimisticLock(entityType, true);
    }

    @NewChain
    BatchEntitySaveCommand<E> setPessimisticLockAll();

    /**
     * Example: <pre>{@code
     *  sqlClient
     *      .getEntities()
     *      .saveEntitiesCommand(
     *          Arrays.asList(process1, process2, process3)
     *      )
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
    default <T extends Table<E>> BatchEntitySaveCommand<E> setOptimisticLock(
            Class<T> tableType,
            UserOptimisticLock<E, T> block
    ) {
        return setOptimisticLock(tableType, UnloadedVersionBehavior.IGNORE, block);
    }

    /**
     * Example: <pre>{@code
     *  sqlClient
     *      .getEntities()
     *      .saveEntitiesCommand(
     *          Arrays.asList(process1, process2, process3)
     *      )
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
    <T extends Table<E>> BatchEntitySaveCommand<E> setOptimisticLock(
            Class<T> tableType,
            UnloadedVersionBehavior unloadedVersionBehavior,
            UserOptimisticLock<E, T> block
    );

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setDeleteMode(DeleteMode mode);

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setMaxCommandJoinCount(int count);

    @Override
    default BatchEntitySaveCommand<E> setDumbBatchAcceptable() {
        return setDumbBatchAcceptable(true);
    }

    @Override
    BatchEntitySaveCommand<E> setDumbBatchAcceptable(boolean acceptable);

    @Override
    BatchEntitySaveCommand<E> setConstraintViolationTranslatable(boolean transferable);

    @NewChain
    @Override
    BatchEntitySaveCommand<E> addExceptionTranslator(ExceptionTranslator<?> translator);

    @Override
    BatchEntitySaveCommand<E> setTransactionRequired(boolean required);

    default BatchSaveResult<E> execute() {
        return execute(null, (Fetcher<E>) null);
    }

    default BatchSaveResult<E> execute(Connection con) {
        return execute(con, (Fetcher<E>) null);
    }

    default BatchSaveResult<E> execute(Fetcher<E> fetcher) {
        return execute(null, fetcher);
    }

    default <V extends View<E>> BatchSaveResult.View<E, V> execute(Class<V> viewType) {
        return execute(null, viewType);
    }

    BatchSaveResult<E> execute(Connection con, Fetcher<E> fetcher);

    <V extends View<E>> BatchSaveResult.View<E, V> execute(Connection con, Class<V> viewType);
}
