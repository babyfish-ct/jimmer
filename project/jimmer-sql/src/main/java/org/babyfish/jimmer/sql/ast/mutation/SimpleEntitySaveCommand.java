package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;

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

    @Override
    default SimpleEntitySaveCommand<E> setAssociatedModeAll(AssociatedSaveMode mode) {
        return configure(cfg -> cfg.setAssociatedModeAll(mode));
    }

    @Override
    default SimpleEntitySaveCommand<E> setAssociatedMode(ImmutableProp prop, AssociatedSaveMode mode) {
        return configure(cfg -> cfg.setAssociatedMode(prop, mode));
    }

    @Override
    default SimpleEntitySaveCommand<E> setAssociatedMode(TypedProp.Association<?, ?> prop, AssociatedSaveMode mode) {
        return configure(cfg -> cfg.setAssociatedMode(prop, mode));
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
    default SimpleEntitySaveCommand<E> setAutoIdOnlyTargetCheckingAll() {
        return configure(Cfg::setAutoIdOnlyTargetCheckingAll);
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop) {
        return configure(cfg -> cfg.setAutoIdOnlyTargetChecking(prop));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop, boolean checking) {
        return configure(cfg -> cfg.setAutoIdOnlyTargetChecking(prop, checking));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop) {
        return configure(cfg -> cfg.setAutoIdOnlyTargetChecking(prop));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop, boolean checking) {
        return configure(cfg -> cfg.setAutoIdOnlyTargetChecking(prop, checking));
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

    @Override
    default SimpleEntitySaveCommand<E> setTargetTransferMode(
            TypedProp.ReferenceList<?, ?> prop,
            TargetTransferMode mode
    ) {
        return configure(cfg -> cfg.setTargetTransferMode(prop, mode));
    }

    @Override
    default SimpleEntitySaveCommand<E> setTargetTransferMode(
            ImmutableProp prop,
            TargetTransferMode mode
    ) {
        return configure(cfg -> cfg.setTargetTransferMode(prop, mode));
    }

    @Override
    default SimpleEntitySaveCommand<E> setTargetTransferModeAll(TargetTransferMode mode) {
        return configure(cfg -> cfg.setTargetTransferModeAll(mode));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setLockMode(LockMode lockMode) {
        return configure(cfg -> cfg.setLockMode(lockMode));
    }

    @NewChain
    default <T extends Table<E>> SimpleEntitySaveCommand<E> setOptimisticLock(
            Class<T> tableType,
            UserOptimisticLock<E, T> block
    ) {
        return configure(cfg -> cfg.setOptimisticLock(tableType, block));
    }

    @NewChain
    default SimpleEntitySaveCommand<E> setDeleteMode(DeleteMode mode) {
        return configure(cfg -> cfg.setDeleteMode(mode));
    }
}
