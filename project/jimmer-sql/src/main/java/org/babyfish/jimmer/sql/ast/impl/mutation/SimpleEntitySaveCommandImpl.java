package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.Arrays;

public class SimpleEntitySaveCommandImpl<E>
        extends AbstractEntitySaveCommandImpl
        implements SimpleEntitySaveCommand<E> {

    public SimpleEntitySaveCommandImpl(
            JSqlClientImplementor sqlClient,
            Connection con,
            E entity
    ) {
        super(initialCfg(sqlClient, con, entity));
    }

    private SimpleEntitySaveCommandImpl(Cfg cfg) {
        super(cfg);
    }

    @Override
    public SimpleSaveResult<E> execute(Connection con) {
        SaveOptions options = options();
        return options.getSqlClient()
                .getConnectionManager()
                .execute(con == null ? options.getConnection() : con, this::executeImpl);
    }

    @SuppressWarnings("unchecked")
    private SimpleSaveResult<E> executeImpl(Connection con) {
        OptionsImpl options = options();
        ImmutableSpi entity = options.getArument();
        Saver saver = new Saver(options, con, entity.__type());
        return saver.save((E)entity);
    }

    private static Cfg initialCfg(JSqlClientImplementor sqlClient, Connection con, Object entity) {
        if (!(entity instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("entity must be an immutable object");
        }
        if (!((ImmutableSpi) entity).__type().isEntity()) {
            throw new IllegalArgumentException("the object is not instance of entity class");
        }
        if (entity instanceof DraftSpi) {
            throw new IllegalArgumentException("entity cannot be a draft object");
        }
        Cfg cfg = new RootCfg(sqlClient, entity);
        if (con != null) {
            cfg = new ConnectionCfg(cfg, con);
        }
        return cfg;
    }

    @Override
    public SimpleEntitySaveCommand<E> setMode(SaveMode mode) {
        return new SimpleEntitySaveCommandImpl<>(new ModeCfg(cfg, mode));
    }

    @Override
    public SimpleEntitySaveCommand<E> setAssociatedModeAll(AssociatedSaveMode mode) {
        return new SimpleEntitySaveCommandImpl<>(new AssociatedModeCfg(cfg, mode));
    }

    @Override
    public SimpleEntitySaveCommand<E> setAssociatedMode(ImmutableProp prop, AssociatedSaveMode mode) {
        return new SimpleEntitySaveCommandImpl<>(new AssociatedModeCfg(cfg, prop, mode));
    }

    @Override
    public SimpleEntitySaveCommand<E> setKeyProps(String group, ImmutableProp... props) {
        return new SimpleEntitySaveCommandImpl<>(new KeyGroupsCfg(cfg, group, Arrays.asList(props)));
    }

    @Override
    public SimpleEntitySaveCommand<E> setUpsertMask(UpsertMask<?> mask) {
        return new SimpleEntitySaveCommandImpl<>(new UpsertMaskCfg(cfg, mask));
    }

    @Override
    public SimpleEntitySaveCommand<E> setAutoIdOnlyTargetCheckingAll() {
        return new SimpleEntitySaveCommandImpl<>(new IdOnlyAutoCheckingCfg(cfg, true));
    }

    @Override
    public SimpleEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop, boolean checking) {
        return new SimpleEntitySaveCommandImpl<>(new IdOnlyAutoCheckingCfg(cfg, prop, checking));
    }

    @Override
    public SimpleEntitySaveCommand<E> setKeyOnlyAsReferenceAll() {
        return new SimpleEntitySaveCommandImpl<>(new KeyOnlyAsReferenceCfg(cfg, true));
    }

    @Override
    public SimpleEntitySaveCommand<E> setKeyOnlyAsReference(ImmutableProp prop, boolean asReference) {
        return new SimpleEntitySaveCommandImpl<>(new KeyOnlyAsReferenceCfg(cfg, prop,asReference));
    }

    @Override
    public SimpleEntitySaveCommand<E> setDissociateAction(ImmutableProp prop, DissociateAction dissociateAction) {
        return new SimpleEntitySaveCommandImpl<>(new DissociationActionCfg(cfg, prop, dissociateAction));
    }

    @Override
    public SimpleEntitySaveCommand<E> setTargetTransferMode(ImmutableProp prop, TargetTransferMode mode) {
        return new SimpleEntitySaveCommandImpl<>(new TargetTransferModeCfg(cfg, prop, mode));
    }

    @Override
    public SimpleEntitySaveCommand<E> setTargetTransferModeAll(TargetTransferMode mode) {
        return new SimpleEntitySaveCommandImpl<>(new TargetTransferModeCfg(cfg, mode));
    }

    @Override
    public SimpleEntitySaveCommand<E> setPessimisticLock(Class<?> entityType, boolean lock) {
        return new SimpleEntitySaveCommandImpl<>(new PessimisticLockCfg(cfg, entityType, lock));
    }

    @Override
    public SimpleEntitySaveCommand<E> setPessimisticLockAll() {
        return new SimpleEntitySaveCommandImpl<>(new PessimisticLockCfg(cfg, true));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Table<E>> SimpleEntitySaveCommand<E> setOptimisticLock(
            Class<T> tableType,
            UnloadedVersionBehavior behavior,
            UserOptimisticLock<E, T> block
    ) {
        return new SimpleEntitySaveCommandImpl<>(
                new OptimisticLockLambdaCfg(
                        cfg,
                        ImmutableType.get(tableType),
                        behavior,
                        (UserOptimisticLock<Object, Table<Object>>) block
                )
        );
    }

    @Override
    public SimpleEntitySaveCommand<E> setEntityOptimisticLock(
            ImmutableType type,
            UnloadedVersionBehavior behavior,
            UserOptimisticLock<Object, Table<Object>> block
    ) {
        return new SimpleEntitySaveCommandImpl<>(
                new OptimisticLockLambdaCfg(
                        cfg,
                        type,
                        behavior,
                        block
                )
        );
    }

    @Override
    public SimpleEntitySaveCommand<E> setDeleteMode(DeleteMode mode) {
        return new SimpleEntitySaveCommandImpl<>(new DeleteModeCfg(cfg, mode));
    }

    @Override
    public SimpleEntitySaveCommand<E> setMaxCommandJoinCount(int count) {
        return new SimpleEntitySaveCommandImpl<>(new MaxCommandJoinCountCfg(cfg, count));
    }

    @Override
    public SimpleEntitySaveCommand<E> setDumbBatchAcceptable(boolean acceptable) {
        return new SimpleEntitySaveCommandImpl<>(new DumbBatchAcceptableCfg(cfg, acceptable));
    }

    @Override
    public SimpleEntitySaveCommand<E> addExceptionTranslator(ExceptionTranslator<?> translator) {
        if (translator == null) {
            return this;
        }
        return new SimpleEntitySaveCommandImpl<>(new ExceptionTranslatorCfg(cfg, translator));
    }
}
