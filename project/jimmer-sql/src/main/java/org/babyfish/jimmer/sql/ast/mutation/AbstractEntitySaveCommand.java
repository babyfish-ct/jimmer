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

    /**
     * Will be deleted in 1.0
     */
    @Deprecated
    @NewChain
    AbstractEntitySaveCommand setAutoAttachingAll();

    /**
     * Will be deleted in 1.0
     */
    @Deprecated
    @NewChain
    default AbstractEntitySaveCommand setAutoAttaching(TypedProp.Association<?, ?> prop) {
        return setAutoAttaching(prop.unwrap());
    }

    /**
     * Will be deleted in 1.0
     */
    @Deprecated
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

        /**
         * Will be deleted in 1.0
         */
        @Deprecated
        @OldChain
        Cfg setAutoAttachingAll();

        /**
         * Will be deleted in 1.0
         */
        @Deprecated
        @OldChain
        default Cfg setAutoAttaching(TypedProp.Association<?, ?> prop) {
            return setAutoAttaching(prop.unwrap());
        }

        /**
         * Will be deleted in 1.0
         */
        @Deprecated
        @OldChain
        Cfg setAutoAttaching(ImmutableProp prop);

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
        default Cfg setAppendOnly(TypedProp.Association<?, ?> prop) {
            return setAppendOnly(prop.unwrap());
        }

        @OldChain
        Cfg setAppendOnly(ImmutableProp prop);

        @OldChain
        Cfg setAppendOnlyAll();

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
