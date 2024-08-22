package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.Arrays;
import java.util.function.Consumer;

public interface AbstractEntitySaveCommand {

    @NewChain
    AbstractEntitySaveCommand configure(Consumer<Cfg> block);

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
    AbstractEntitySaveCommand setKeyProps(TypedProp<?, ?> ... props);

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
    AbstractEntitySaveCommand setLockMode(LockMode lockMode);

    interface Cfg {

        @OldChain
        Cfg setMode(SaveMode mode);

        @OldChain
        Cfg setAssociatedModeAll(AssociatedSaveMode mode);

        @OldChain
        Cfg setAssociatedMode(ImmutableProp prop, AssociatedSaveMode mode);

        @OldChain
        Cfg setAssociatedMode(TypedProp.Association<?, ?> prop, AssociatedSaveMode mode);

        @OldChain
        Cfg setKeyProps(ImmutableProp ... props);

        @OldChain
        default Cfg setKeyProps(TypedProp<?, ?> ... props) {
            return setKeyProps(
                    Arrays
                            .stream(props)
                            .map(TypedProp::unwrap)
                            .toArray(ImmutableProp[]::new)
            );
        }

        @OldChain
        Cfg setAutoIdOnlyTargetCheckingAll();

        @OldChain
        default Cfg setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop) {
            return setAutoIdOnlyTargetChecking(prop.unwrap(), true);
        }

        @OldChain
        default Cfg setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop, boolean checking) {
            return setAutoIdOnlyTargetChecking(prop.unwrap(), checking);
        }

        @OldChain
        default Cfg setAutoIdOnlyTargetChecking(ImmutableProp prop) {
            return setAutoIdOnlyTargetChecking(prop, true);
        }

        @OldChain
        Cfg setAutoIdOnlyTargetChecking(ImmutableProp prop, boolean checking);

        @OldChain
        default Cfg setDissociateAction(
                TypedProp.Reference<?, ?> prop,
                DissociateAction dissociateAction
        ) {
            return setDissociateAction(prop.unwrap(), dissociateAction);
        }

        @OldChain
        Cfg setDissociateAction(
                ImmutableProp prop,
                DissociateAction dissociateAction
        );

        @OldChain
        default Cfg setTargetTransferMode(TypedProp.ReferenceList<?, ?> prop, TargetTransferMode mode) {
            return setTargetTransferMode(prop.unwrap(), mode);
        }

        @OldChain
        Cfg setTargetTransferMode(ImmutableProp prop, TargetTransferMode mode);

        @OldChain
        Cfg setTargetTransferModeAll(TargetTransferMode mode);

        @OldChain
        Cfg setLockMode(LockMode lockMode);

        @OldChain
        <E, T extends Table<E>> Cfg setOptimisticLock(
                Class<T> tableType,
                UserOptimisticLock<E, T> block
        );

        @OldChain
        Cfg setDeleteMode(DeleteMode mode);
    }

    interface KeyPropCfg<T> {

        @OldChain
        KeyPropCfg<T> add(ImmutableProp prop);

        @OldChain
        KeyPropCfg<T> add(TypedProp<?, ?> prop);
    }
}
