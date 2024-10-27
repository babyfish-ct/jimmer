package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;

import java.util.Collection;

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
    BatchEntitySaveCommand<E> setKeyProps(ImmutableProp... props);

    @NewChain
    @Override
    default BatchEntitySaveCommand<E> setKeyProps(TypedProp<?, ?>... props) {
        ImmutableProp[] unwrappedProps = new ImmutableProp[props.length];
        for (int i = 0; i < props.length; i++) {
            unwrappedProps[i] = props[i].unwrap();
        }
        return setKeyProps(unwrappedProps);
    }

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
    @Override
    BatchEntitySaveCommand<E> setLockMode(LockMode lockMode);

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
    <T extends Table<E>> BatchEntitySaveCommand<E> setOptimisticLock(
            Class<T> tableType,
            UserOptimisticLock<E, T> block
    );

    @NewChain
    @Override
    BatchEntitySaveCommand<E> setDeleteMode(DeleteMode mode);

    @NewChain
    @Override
    BatchEntitySaveCommand<E> addExceptionTranslator(ExceptionTranslator<?> translator);
}
