package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiFunction;
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

    /**
     * Will be removed in 1.0
     * @deprecated Now, it is default behavior, this method is unnecessary
     */
    @Deprecated
    @NewChain
    default BatchEntitySaveCommand<E> setAutoAttachingAll() {
        return configure(Cfg::setAutoAttachingAll);
    }

    /**
     * Will be removed in 1.0
     * @deprecated Now, it is default behavior, this method is unnecessary
     */
    @Deprecated
    @NewChain
    default BatchEntitySaveCommand<E> setAutoAttaching(TypedProp.Association<?, ?> prop) {
        return configure(cfg -> cfg.setAutoAttaching(prop));
    }

    /**
     * Will be removed in 1.0
     * @deprecated Now, it is default behavior, this method is unnecessary
     */
    @Deprecated
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
    default BatchEntitySaveCommand<E> setAutoIdOnlyTargetChecking(TypedProp.Association<?, ?> prop, boolean checking) {
        return configure(cfg -> cfg.setAutoIdOnlyTargetChecking(prop, checking));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop) {
        return configure(cfg -> cfg.setAutoIdOnlyTargetChecking(prop));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop, boolean checking) {
        return configure(cfg -> cfg.setAutoIdOnlyTargetChecking(prop, checking));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setMergeMode() {
        return configure(cfg -> cfg.setMergeMode(true));
    }

    /**
     * Enable/Disable the merge mode.
     *
     * <p>
     *     If the merge mode is enabled, for each object of the save tree,
     *     only insert and update operations will be executed,
     *     dissociation operations will never be executed.
     * </p>
     * @param mergeMode is merge mode enable
     * @return A new save command
     */
    @NewChain
    default BatchEntitySaveCommand<E> setMergeMode(boolean mergeMode) {
        return configure(cfg -> cfg.setMergeMode(mergeMode));
    }

    /**
     * Will be removed in 1.0
     * @deprecated Please use {@link #setMergeMode(boolean)} or {@link #setMergeMode()}
     */
    @Deprecated
    @NewChain
    default BatchEntitySaveCommand<E> setAppendOnlyAll() {
        return configure(Cfg::setAppendOnlyAll);
    }

    /**
     * Will be removed in 1.0
     * @deprecated Please use {@link #setMergeMode(boolean)} or {@link #setMergeMode()}
     */
    @Deprecated
    @NewChain
    default BatchEntitySaveCommand<E> setAppendOnly(TypedProp.Association<?, ?> prop) {
        return configure(cfg -> cfg.setAppendOnly(prop));
    }

    /**
     * Will be removed in 1.0
     * @deprecated Please use {@link #setMergeMode(boolean)} or {@link #setMergeMode()}
     */
    @Deprecated
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
    default BatchEntitySaveCommand<E> setLockMode(LockMode lockMode) {
        return configure(cfg -> cfg.setLockMode(lockMode));
    }

    @NewChain
    default <T extends Table<E>> BatchEntitySaveCommand<E> setOptimisticLock(
            Class<T> tableType,
            BiFunction<T, E, Predicate> block
    ) {
        return configure(cfg -> cfg.setOptimisticLock(tableType, block));
    }

    @NewChain
    default BatchEntitySaveCommand<E> setDeleteMode(DeleteMode mode) {
        return configure(cfg -> cfg.setDeleteMode(mode));
    }
}
