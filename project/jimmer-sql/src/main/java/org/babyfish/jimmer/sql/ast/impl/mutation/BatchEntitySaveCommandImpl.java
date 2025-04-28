package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.DtoMetadata;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;
import org.babyfish.jimmer.sql.runtime.Executor;
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
    public BatchEntitySaveCommand<E> setKeyProps(String group, ImmutableProp... props) {
        return new BatchEntitySaveCommandImpl<>(new KeyGroupsCfg(cfg, group, Arrays.asList(props)));
    }

    @Override
    public BatchEntitySaveCommand<E> setUpsertMask(UpsertMask<?> mask) {
        return new BatchEntitySaveCommandImpl<>(new UpsertMaskCfg(cfg, mask));
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
    public BatchEntitySaveCommand<E> setIdOnlyAsReferenceAll(boolean asReference) {
        return new BatchEntitySaveCommandImpl<>(new IdOnlyAsReferenceCfg(cfg, asReference));
    }

    @Override
    public BatchEntitySaveCommand<E> setIdOnlyAsReference(ImmutableProp prop, boolean asReference) {
        return new BatchEntitySaveCommandImpl<>(new IdOnlyAsReferenceCfg(cfg, prop, asReference));
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
    public BatchEntitySaveCommand<E> setPessimisticLock(Class<?> entityType, boolean lock) {
        return new BatchEntitySaveCommandImpl<>(new PessimisticLockCfg(cfg, entityType, lock));
    }

    @Override
    public BatchEntitySaveCommand<E> setPessimisticLockAll() {
        return new BatchEntitySaveCommandImpl<>(new PessimisticLockCfg(cfg, true));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Table<E>> BatchEntitySaveCommand<E> setOptimisticLock(
            Class<T> tableType,
            UnloadedVersionBehavior behavior,
            UserOptimisticLock<E, T> block
    ) {
        return new BatchEntitySaveCommandImpl<>(
                new OptimisticLockLambdaCfg(
                        cfg,
                        ImmutableType.get(tableType),
                        behavior,
                        (UserOptimisticLock<Object, Table<Object>>) block
                )
        );
    }

    @Override
    public BatchEntitySaveCommand<E> setEntityOptimisticLock(
            ImmutableType type,
            UnloadedVersionBehavior behavior,
            UserOptimisticLock<Object, Table<Object>> block
    ) {
        return new BatchEntitySaveCommandImpl<>(
                new OptimisticLockLambdaCfg(
                        cfg,
                        type,
                        behavior,
                        block
                )
        );
    }

    @Override
    public BatchEntitySaveCommand<E> setDeleteMode(DeleteMode mode) {
        return new BatchEntitySaveCommandImpl<>(new DeleteModeCfg(cfg, mode));
    }

    @Override
    public BatchEntitySaveCommand<E> setMaxCommandJoinCount(int count) {
        return new BatchEntitySaveCommandImpl<>(new MaxCommandJoinCountCfg(cfg, count));
    }

    @Override
    public BatchEntitySaveCommand<E> setDumbBatchAcceptable(boolean acceptable) {
        return new BatchEntitySaveCommandImpl<>(new DumbBatchAcceptableCfg(cfg, acceptable));
    }

    @Override
    public BatchEntitySaveCommand<E> setConstraintViolationTranslatable(boolean transferable) {
        return new BatchEntitySaveCommandImpl<>(new ConstraintViolationTranslatableCfg(cfg, transferable));
    }

    @Override
    public BatchEntitySaveCommand<E> addExceptionTranslator(ExceptionTranslator<?> translator) {
        if (translator == null) {
            return this;
        }
        return new BatchEntitySaveCommandImpl<>(new ExceptionTranslatorCfg(cfg, translator));
    }

    @Override
    public BatchEntitySaveCommand<E> setTransactionRequired(boolean required) {
        return new BatchEntitySaveCommandImpl<>(new TransactionRequiredCfg(cfg, required));
    }

    private static <E> Collection<E> entities(OptionsImpl options) {
        Iterable<E> iterable = options.getArument();
        if (iterable instanceof Collection<?>) {
            return (Collection<E>) iterable;
        }
        List<E> list = new ArrayList<>();
        for (E e : iterable) {
            list.add(e);
        }
        return list;
    }

    @Override
    public BatchSaveResult<E> execute(Connection con, Fetcher<E> fetcher) {
        OptionsImpl options = options();
        Collection<E> entities = entities(options);
        if (entities.isEmpty()) {
            return new BatchSaveResult<>(Collections.emptyMap(), Collections.emptyList());
        }
        return options
                .getSqlClient()
                .getConnectionManager()
                .execute(
                        con == null ? options.getConnection() : con,
                        c -> executeImpl(c, fetcher)
                );
    }

    @Override
    public <V extends View<E>> BatchSaveResult.View<E, V> execute(Connection con, Class<V> viewType) {
        OptionsImpl options = options();
        DtoMetadata<E, V> metadata = DtoMetadata.of(viewType);
        Collection<E> entities = entities(options);
        if (entities.isEmpty()) {
            return new BatchSaveResult<E>(
                    Collections.emptyMap(),
                    Collections.emptyList()
            ).toView(metadata.getConverter());
        }
        BatchSaveResult<E> result = options
                .getSqlClient()
                .getConnectionManager()
                .execute(
                        con == null ? options.getConnection() : con,
                        c -> executeImpl(c, metadata.getFetcher())
                );
        return result.toView(metadata.getConverter());
    }

    private BatchSaveResult<E> executeImpl(Connection con, Fetcher<E> fetcher) {

        OptionsImpl options = options();
        if (options.isTransactionRequired()) {
            Executor.validateMutationConnection(con);
        }
        Collection<E> entities = entities(options);
        ImmutableType type = ImmutableType.get(entities.iterator().next().getClass());
        Saver saver = new Saver(
                options,
                con,
                type,
                fetcher
        );
        return saver.saveAll(entities);
    }
}
