package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.*;

public class BatchEntitySaveCommandImpl<E>
        extends AbstractEntitySaveCommandImpl
        implements BatchEntitySaveCommand<E> {

    public BatchEntitySaveCommandImpl(
            JSqlClientImplementor sqlClient,
            Connection con,
            Iterable<E> entities
    ) {
        super(initialCfg(sqlClient, con, entities));
    }

    private BatchEntitySaveCommandImpl(Cfg cfg) {
        super(cfg);
    }

    @Override
    public BatchSaveResult<E> execute(Connection con) {
        OptionsImpl options = options();
        List<E> entities = options.getArument();
        if (entities.isEmpty()) {
            return new BatchSaveResult<>(Collections.emptyList());
        }
        return options
                .getSqlClient()
                .getConnectionManager()
                .execute(con == null ? options.getConnection() : con, this::executeImpl);
    }

    private BatchSaveResult<E> executeImpl(Connection con) {
        OptionsImpl options = options();
        List<E> entities = options.getArument();
        ImmutableType type = ImmutableType.get(entities.iterator().next().getClass());
        Saver saver = new Saver(options, con, type);
        return saver.saveAll(entities);
    }

    private static Cfg initialCfg(JSqlClientImplementor sqlClient, Connection con, Iterable<?> entities) {
        ImmutableType type = null;
        for (Object entity : entities) {
            if (!(entity instanceof ImmutableSpi)) {
                throw new IllegalArgumentException(
                        "All the elements of entities must be immutable object"
                );
            }
            if (!((ImmutableSpi) entity).__type().isEntity()) {
                throw new IllegalArgumentException(
                        "All the elements must be entity object"
                );
            }
            if (entity instanceof DraftSpi) {
                throw new IllegalArgumentException("Each element of entity cannot be draft object");
            }
            ImmutableType entityType = ((ImmutableSpi) entity).__type();
            if (type != null && entityType != type) {
                throw new IllegalArgumentException(
                        "All the elements of entities must belong to same immutable type"
                );
            }
            type = entityType;
        }
        Cfg cfg = new RootCfg(sqlClient, entities);
        if (con != null) {
            cfg = new ConnectionCfg(cfg, con);
        }
        return cfg;
    }

    @Override
    public BatchEntitySaveCommand<E> setMode(SaveMode mode) {
        return new BatchEntitySaveCommandImpl<>(new ModeCfg(cfg, mode));
    }

    @Override
    public BatchEntitySaveCommand<E> setAssociatedModeAll(AssociatedSaveMode mode) {
        return new BatchEntitySaveCommandImpl<>(new AssociatedModeCfg(cfg, mode));
    }

    @Override
    public BatchEntitySaveCommand<E> setAssociatedMode(ImmutableProp prop, AssociatedSaveMode mode) {
        return new BatchEntitySaveCommandImpl<>(new AssociatedModeCfg(cfg, prop, mode));
    }

    @Override
    public BatchEntitySaveCommand<E> setKeyProps(ImmutableProp... props) {
        return new BatchEntitySaveCommandImpl<>(new KeyGroupsCfg(cfg, "", Arrays.asList(props)));
    }

    @Override
    public BatchEntitySaveCommand<E> setKeyProps(String group, ImmutableProp... props) {
        return new BatchEntitySaveCommandImpl<>(new KeyGroupsCfg(cfg, group, Arrays.asList(props)));
    }

    @Override
    public BatchEntitySaveCommand<E> setAutoIdOnlyTargetCheckingAll() {
        return new BatchEntitySaveCommandImpl<>(new IdOnlyAutoCheckingCfg(cfg, true));
    }

    @Override
    public BatchEntitySaveCommand<E> setAutoIdOnlyTargetChecking(ImmutableProp prop, boolean checking) {
        return new BatchEntitySaveCommandImpl<>(new IdOnlyAutoCheckingCfg(cfg, prop, checking));
    }

    @Override
    public BatchEntitySaveCommand<E> setKeyOnlyAsReferenceAll() {
        return new BatchEntitySaveCommandImpl<>(new KeyOnlyAsReferenceCfg(cfg, true));
    }

    @Override
    public BatchEntitySaveCommand<E> setKeyOnlyAsReference(ImmutableProp prop, boolean asReference) {
        return new BatchEntitySaveCommandImpl<>(new KeyOnlyAsReferenceCfg(cfg, prop,asReference));
    }

    @Override
    public BatchEntitySaveCommand<E> setDissociateAction(ImmutableProp prop, DissociateAction dissociateAction) {
        return new BatchEntitySaveCommandImpl<>(new DissociationActionCfg(cfg, prop, dissociateAction));
    }

    @Override
    public BatchEntitySaveCommand<E> setTargetTransferMode(ImmutableProp prop, TargetTransferMode mode) {
        return new BatchEntitySaveCommandImpl<>(new TargetTransferModeCfg(cfg, prop, mode));
    }

    @Override
    public BatchEntitySaveCommand<E> setTargetTransferModeAll(TargetTransferMode mode) {
        return new BatchEntitySaveCommandImpl<>(new TargetTransferModeCfg(cfg, mode));
    }

    @Override
    public BatchEntitySaveCommand<E> setLockMode(LockMode lockMode) {
        return new BatchEntitySaveCommandImpl<>(new LockModeCfg(cfg, lockMode));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Table<E>> BatchEntitySaveCommand<E> setOptimisticLock(
            Class<T> tableType,
            UserOptimisticLock<E, T> block
    ) {
        return new BatchEntitySaveCommandImpl<>(
                new OptimisticLockLambdaCfg(
                        cfg,
                        ImmutableType.get(tableType),
                        (UserOptimisticLock<Object, Table<Object>>) block
                )
        );
    }

    @Override
    public BatchEntitySaveCommand<E> setEntityOptimisticLock(ImmutableType type, UserOptimisticLock<Object, Table<Object>> block) {
        return new BatchEntitySaveCommandImpl<>(
                new OptimisticLockLambdaCfg(
                        cfg,
                        type,
                        block
                )
        );
    }

    @Override
    public BatchEntitySaveCommand<E> setDeleteMode(DeleteMode mode) {
        return new BatchEntitySaveCommandImpl<>(new DeleteModeCfg(cfg, mode));
    }

    @Override
    public BatchEntitySaveCommand<E> setInvestigateKeyBasedUpdate(boolean investigate) {
        return new BatchEntitySaveCommandImpl<>(new InvestigateKeyBasedUpdateCfg(cfg, investigate));
    }

    @Override
    public BatchEntitySaveCommand<E> addExceptionTranslator(ExceptionTranslator<?> translator) {
        if (translator == null) {
            return this;
        }
        return new BatchEntitySaveCommandImpl<>(new ExceptionTranslatorCfg(cfg, translator));
    }
}
