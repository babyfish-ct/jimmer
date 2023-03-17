package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Executable;

import java.util.function.Consumer;

public interface BatchEntitySaveCommand<E>
        extends Executable<BatchSaveResult<E>>,
        AbstractEntitySaveCommand {

    @Override
    @NewChain
    BatchEntitySaveCommand<E> configure(Consumer<Cfg> block);

    @NewChain
    default BatchEntitySaveCommand<E> setMode(SaveMode mode) {
        return configure(cfg -> cfg.setMode(mode));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setKeyProps(ImmutableProp... props) {
        return configure(cfg -> cfg.setKeyProps(props));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setKeyProps(TypedProp<?, ?>... props) {
        return configure(cfg -> cfg.setKeyProps(props));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setAutoAttachingAll() {
        return configure(Cfg::setAutoAttachingAll);
    }

    @NewChain
    default BatchEntitySaveCommand<E> setAutoAttaching(TypedProp.Association<?, ?> prop) {
        return configure(cfg -> cfg.setAutoAttaching(prop));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setAutoAttaching(ImmutableProp prop) {
        return configure(cfg -> cfg.setAutoAttaching(prop));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setAutoIdOnlyTargetCheckingAll() {
        return configure(Cfg::setAutoIdOnlyTargetCheckingAll);
    }

    @NewChain
    default BatchEntitySaveCommand<E> setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop) {
        return configure(cfg -> cfg.setAutoIdOnlyTargetChecking(prop));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop) {
        return configure(cfg -> cfg.setAutoIdOnlyTargetChecking(prop));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setAppendOnlyAll() {
        return configure(Cfg::setAppendOnlyAll);
    }

    @NewChain
    default BatchEntitySaveCommand<E> setAppendOnly(TypedProp.Association<?, ?> prop) {
        return configure(cfg -> cfg.setAppendOnly(prop));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setAppendOnly(ImmutableProp prop) {
        return configure(cfg -> cfg.setAppendOnly(prop));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setDissociateAction(
            TypedProp.Reference<?, ?> prop,
            DissociateAction dissociateAction
    ) {
        return configure(cfg -> cfg.setDissociateAction(prop, dissociateAction));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setDissociateAction(
            ImmutableProp prop,
            DissociateAction dissociateAction
    ) {
        return configure(cfg -> cfg.setDissociateAction(prop, dissociateAction));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setPessimisticLock() {
        return configure(Cfg::setPessimisticLock);
    }

    @NewChain
    default BatchEntitySaveCommand<E> setPessimisticLock(boolean pessimisticLock) {
        return configure(cfg -> cfg.setPessimisticLock(pessimisticLock));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setDeleteMode(DeleteMode mode) {
        return configure(cfg -> cfg.setDeleteMode(mode));
    }
}
