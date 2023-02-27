package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Executable;

import java.util.function.Consumer;

public interface SimpleEntitySaveCommand<E>
        extends Executable<SimpleSaveResult<E>>,
        AbstractEntitySaveCommand {

    @Override
    @NewChain
    SimpleEntitySaveCommand<E> configure(Consumer<Cfg> block);

    @NewChain
    default SimpleEntitySaveCommand<E> setMode(SaveMode mode) {
        return configure(cfg -> cfg.setMode(mode));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setKeyProps(ImmutableProp... props) {
        return configure(cfg -> cfg.setKeyProps(props));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setKeyProps(TypedProp<?, ?>... props) {
        return configure(cfg -> cfg.setKeyProps(props));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setAutoAttachingAll() {
        return configure(Cfg::setAutoAttachingAll);
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setAutoAttaching(TypedProp.Association<?, ?> prop) {
        return configure(cfg -> cfg.setAutoAttaching(prop));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setAutoAttaching(ImmutableProp prop) {
        return configure(cfg -> cfg.setAutoAttaching(prop));
    }

    @OldChain
    default SimpleEntitySaveCommand<E> setAutoIdOnlyTargetCheckingAll() {
        return configure(Cfg::setAutoIdOnlyTargetCheckingAll);
    }

    @OldChain
    default SimpleEntitySaveCommand<E> setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop) {
        return configure(cfg -> cfg.setAutoIdOnlyTargetChecking(prop));
    }

    @OldChain
    default SimpleEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop) {
        return configure(cfg -> cfg.setAutoIdOnlyTargetChecking(prop));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setDissociateAction(
            TypedProp.Reference<?, ?> prop,
            DissociateAction dissociateAction
    ) {
        return configure(cfg -> cfg.setDissociateAction(prop, dissociateAction));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setDissociateAction(
            ImmutableProp prop,
            DissociateAction dissociateAction
    ) {
        return configure(cfg -> cfg.setDissociateAction(prop, dissociateAction));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setPessimisticLock() {
        return configure(Cfg::setPessimisticLock);
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setPessimisticLock(boolean pessimisticLock) {
        return configure(cfg -> cfg.setPessimisticLock(pessimisticLock));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setDeleteMode(DeleteMode mode) {
        return configure(cfg -> cfg.setDeleteMode(mode));
    }
}
