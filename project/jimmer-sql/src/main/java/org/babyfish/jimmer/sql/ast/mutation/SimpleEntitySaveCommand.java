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
    SimpleEntitySaveCommand<E> setKeyProps(ImmutableProp... props);

    @NewChain
    @Override
    default SimpleEntitySaveCommand<E> setKeyProps(TypedProp<?, ?>... props) {
        ImmutableProp[] unwrappedProps = new ImmutableProp[props.length];
        for (int i = 0; i < props.length; i++) {
            unwrappedProps[i] = props[i].unwrap();
        }
        return setKeyProps(unwrappedProps);
    }

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
    @Override
    SimpleEntitySaveCommand<E> setLockMode(LockMode lockMode);

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
    <T extends Table<E>> SimpleEntitySaveCommand<E> setOptimisticLock(
            Class<T> tableType,
            UserOptimisticLock<E, T> block
    );

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> setDeleteMode(DeleteMode mode);

    @NewChain
    @Override
    SimpleEntitySaveCommand<E> addExceptionTranslator(ExceptionTranslator<?> translator);
}
