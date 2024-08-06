package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ast.impl.mutation.DeleteOptions;
import org.babyfish.jimmer.sql.ast.impl.mutation.SaveOptions;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;

import java.sql.Connection;
import java.util.*;

public class Saver2 {

    private final SaveContext ctx;

    public Saver2(
            SaveOptions options,
            Connection con,
            ImmutableType type
    ) {
        this(
                new SaveContext(
                        options,
                        con,
                        type
                )
        );
    }

    private Saver2(SaveContext ctx) {
        this.ctx = ctx;
    }

    @SuppressWarnings("unchecked")
    public <E> SimpleSaveResult<E> save(E entity) {
        ImmutableType immutableType = ImmutableType.get(entity.getClass());
        MutationTrigger2 trigger = ctx.trigger;
        E newEntity = (E) Internal.produce(
                immutableType,
                entity,
                draft -> {
                    saveAllImpl(Collections.singletonList((DraftSpi) draft));
                },
                trigger == null ? null : trigger::prepareSubmit
        );
        if (trigger != null) {
            trigger.submit(ctx.options.getSqlClient(), ctx.con);
        }
        return new SimpleSaveResult<>(
                ctx.affectedRowCountMap,
                entity,
                newEntity
        );
    }

    @SuppressWarnings("unchecked")
    public <E> BatchSaveResult<E> saveAll(Collection<E> entities) {
        if (entities.isEmpty()) {
            return new BatchSaveResult<>(Collections.emptyList());
        }
        ImmutableType immutableType = ImmutableType.get(entities.iterator().next().getClass());
        MutationTrigger2 trigger = ctx.trigger;
        List<E> newEntities = (List<E>) Internal.produceList(
                immutableType,
                entities,
                drafts -> {
                    saveAllImpl((List<DraftSpi>) drafts);
                },
                trigger == null ? null : trigger::prepareSubmit
        );
        if (trigger != null) {
            trigger.submit(ctx.options.getSqlClient(), ctx.con);
        }
        Iterator<E> oldItr = entities.iterator();
        Iterator<E> newItr = newEntities.iterator();
        List<SimpleSaveResult<E>> results = new ArrayList<>(entities.size());
        while (oldItr.hasNext() && newItr.hasNext()) {
            results.add(
                    new SimpleSaveResult<>(
                            ctx.affectedRowCountMap,
                            oldItr.next(),
                            newItr.next()
                    )
            );
        }
        return new BatchSaveResult<>(results);
    }

    private void saveAllImpl(List<DraftSpi> drafts) {
        PreHandler preHandler = PreHandler.of(ctx);
        for (DraftSpi draft : drafts) {
            preHandler.add(draft);
        }
        for (Batch<DraftSpi> batch : preHandler.batches()) {
            for (ImmutableProp prop : batch.shape().getGetterMap().keySet()) {
                if (prop.isAssociation(TargetLevel.ENTITY) && prop.isColumnDefinition()) {
                    savePreAssociation(prop, batch);
                }
            }
        }

        saveSelf(preHandler);

        for (Batch<DraftSpi> batch : preHandler.batches()) {
            for (ImmutableProp prop : batch.shape().getGetterMap().keySet()) {
                if (prop.isAssociation(TargetLevel.ENTITY) && !prop.isColumnDefinition()) {
                    savePostAssociation(prop, batch);
                }
            }
        }
    }

    private void savePreAssociation(ImmutableProp prop, Batch<DraftSpi> batch) {
        if (!batch.shape().getIdGetters().isEmpty() && batch.shape().getGetterMap().size() == 1) {
            if (!ctx.options.isAutoCheckingProp(prop)) {
                return;
            }
            Set<Object> targetIds = new HashSet<>();
            PropId targetIdPropId = prop.getTargetType().getIdProp().getId();
            for (DraftSpi draft : batch.entities()) {
                Object targetId = draft.__get(targetIdPropId);
                if (targetId != null) {
                    targetIds.add(targetId);
                }
            }
            MutableRootQueryImpl<Table<Object>> q = new MutableRootQueryImpl<>(
                    ctx.options.getSqlClient(),
                    ctx.path.getType(),
                    ExecutionPurpose.MUTATE,
                    FilterLevel.DEFAULT
            );
            Table<?> table = q.getTableImplementor();
            q.where(table.getId().in(targetIds));
            List<Object> actualTargetIds = q.select(table.getId()).execute(ctx.con);
            if (actualTargetIds.size() < targetIds.size()) {
                actualTargetIds.forEach(targetIds::remove);
                ctx.prop(prop).throwIllegalTargetIds(targetIds);
            }
            return;
        }
        Saver2 targetSaver = new Saver2(ctx.prop(prop));
        List<DraftSpi> targets = new ArrayList<>(batch.entities().size());
        PropId targetPropId = prop.getId();
        for (DraftSpi draft : batch.entities()) {
            DraftSpi target = (DraftSpi) draft.__get(targetPropId);
            if (target != null) {
                targets.add(target);
            }
        }
        targetSaver.saveAll(targets);
    }

    @SuppressWarnings("unchecked")
    private void savePostAssociation(ImmutableProp prop, Batch<DraftSpi> batch) {
        if (isReadOnlyMiddleTable(prop)) {
            ctx.throwReadonlyMiddleTable();
        }
        if (prop.isRemote() && prop.getMappedBy() != null) {
            ctx.throwReversedRemoteAssociation();
        }
        if (prop.getSqlTemplate() instanceof JoinTemplate) {
            ctx.throwUnstructuredAssociation();
        }

        Saver2 targetSaver = new Saver2(ctx.prop(prop));
        List<DraftSpi> targets = new ArrayList<>(batch.entities().size());
        PropId targetPropId = prop.getId();
        for (DraftSpi draft : batch.entities()) {
            Object value = draft.__get(targetPropId);
            if (value instanceof List<?>) {
                targets.addAll((List<DraftSpi>) value);
            } else if (value != null) {
                targets.add((DraftSpi) value);
            }
        }
        targetSaver.saveAll(targets);

        if (ctx.options.getMode() != SaveMode.INSERT_ONLY ||
                ctx.options.getAssociatedMode(prop) == AssociatedSaveMode.REPLACE) {
            replace(batch, prop);
        }
    }

    private void saveSelf(PreHandler preHandler) {
        Operator operator = new Operator(ctx);
        for (Batch<DraftSpi> batch : preHandler.batches()) {
            switch (ctx.options.getMode()) {
                case INSERT_ONLY:
                    operator.insert(batch);
                    break;
                case UPDATE_ONLY:
                    operator.update(
                            preHandler.originalIdObjMap(),
                            preHandler.originalkeyObjMap(),
                            batch
                    );
                    break;
                default:
                    operator.upsert(batch);
                    break;
            }
        }
    }

    private void replace(Batch<DraftSpi> batch, ImmutableProp prop) {
        ChildTableOperator subOperator = null;
        MiddleTableOperator middleTableOperator = null;
        if (prop.isMiddleTableDefinition()) {
            middleTableOperator = new MiddleTableOperator(
                    ctx.prop(prop),
                    ctx.options.getDeleteMode() == DeleteMode.LOGICAL
            );
        } else {
            ImmutableProp mappedBy = prop.getMappedBy();
            if (mappedBy != null) {
                if (mappedBy.isColumnDefinition()) {
                    subOperator = new ChildTableOperator(
                            new DeleteContext(
                                    DeleteOptions.detach(ctx.options),
                                    ctx.con,
                                    ctx.trigger,
                                    ctx.affectedRowCountMap,
                                    ctx.path.to(prop)
                            )
                    );
                } else if (mappedBy.isMiddleTableDefinition()) {
                    middleTableOperator = new MiddleTableOperator(
                            ctx.prop(prop),
                            ctx.options.getDeleteMode() == DeleteMode.LOGICAL
                    );
                }
            }
        }
        if (subOperator == null && middleTableOperator == null) {
            return;
        }
        IdPairs idPairs = IdPairs.of(batch.entities(), prop);
        if (subOperator != null) {
            subOperator.disconnectExcept(idPairs);
        }
        if (middleTableOperator != null) {
            switch (ctx.options.getAssociatedMode(prop)) {
                case APPEND:
                    middleTableOperator.append(idPairs);
                    break;
                case MERGE:
                    middleTableOperator.merge(idPairs);
                    break;
                case REPLACE:
                    middleTableOperator.replace(idPairs);
                    break;
            }
        }
    }

    private boolean isReadOnlyMiddleTable(ImmutableProp prop) {
        ImmutableProp mappedBy = prop.getMappedBy();
        if (mappedBy != null) {
            prop = mappedBy;
        }
        if (prop.isMiddleTableDefinition()) {
            MiddleTable middleTable = prop.getStorage(ctx.options.getSqlClient().getMetadataStrategy());
            return middleTable.isReadonly();
        }
        return false;
    }
}
