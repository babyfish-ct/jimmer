package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;

import java.util.Arrays;
import java.util.function.Consumer;

public interface AbstractEntitySaveCommand {

    @NewChain
    AbstractEntitySaveCommand configure(Consumer<Cfg> block);

    @NewChain
    AbstractEntitySaveCommand setMode(SaveMode mode);

    @NewChain
    AbstractEntitySaveCommand setKeyProps(ImmutableProp ... props);

    @NewChain
    default AbstractEntitySaveCommand setKeyProps(TypedProp<?, ?> ... props) {
        return setKeyProps(
                Arrays
                        .stream(props)
                        .map(TypedProp::unwrap)
                        .toArray(ImmutableProp[]::new)
        );
    }

    @NewChain
    AbstractEntitySaveCommand setAutoAttachingAll();

    @NewChain
    default AbstractEntitySaveCommand setAutoAttaching(TypedProp.Association<?, ?> prop) {
        return setAutoAttaching(prop.unwrap());
    }

    @NewChain
    AbstractEntitySaveCommand setAutoAttaching(ImmutableProp prop);

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
    default AbstractEntitySaveCommand setPessimisticLock() {
        return setPessimisticLock(true);
    }

    @NewChain
    AbstractEntitySaveCommand setPessimisticLock(boolean pessimisticLock);

    @NewChain
    AbstractEntitySaveCommand setDeleteMode(DeleteMode mode);

    interface Cfg {

        @OldChain
        Cfg setMode(SaveMode mode);

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
        Cfg setAutoAttachingAll();

        @OldChain
        default Cfg setAutoAttaching(TypedProp.Association<?, ?> prop) {
            return setAutoAttaching(prop.unwrap());
        }

        @OldChain
        Cfg setAutoAttaching(ImmutableProp prop);

        @OldChain
        Cfg setAutoIdOnlyTargetCheckingAll();

        @OldChain
        default Cfg setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop) {
            return setAutoIdOnlyTargetChecking(prop.unwrap());
        }

        @OldChain
        Cfg setAutoIdOnlyTargetChecking(ImmutableProp prop);

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
        default Cfg setPessimisticLock() {
            return setPessimisticLock(true);
        }

        @OldChain
        Cfg setPessimisticLock(boolean pessimisticLock);

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
