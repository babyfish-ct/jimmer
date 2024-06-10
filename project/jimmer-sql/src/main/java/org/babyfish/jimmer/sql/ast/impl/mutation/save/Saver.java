package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.impl.Utils;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JoinSql;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.util.ConcattedIterator;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SaveException;

import java.util.*;

class Saver {

    private final SaveContext ctx;

    public Saver(SaveContext ctx) {
        this.ctx = ctx;
    }

    @SuppressWarnings("unchecked")
    public <E> void saveAll(Collection<E> entities) {
        if (entities.isEmpty()) {
            return;
        }
        ImmutableType immutableType = ImmutableType.get(entities.iterator().next().getClass());
        MutationTrigger trigger = ctx.trigger;
        E newEntities = (E) Internal.produceList(
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
    }

    private void saveAllImpl(List<DraftSpi> drafts) {
        PreHandler preHandler = PreHandler.of(ctx);
        for (DraftSpi draft : drafts) {
            preHandler.add(draft);
        }
        for (Batch<DraftSpi> batch : preHandler.batches()) {
            for (ImmutableProp prop : batch.shape().getItemMap().keySet()) {
                if (prop.isAssociation(TargetLevel.ENTITY) && prop.isColumnDefinition()) {
                    savePreAssociation(prop, batch);
                }
            }
        }

        saveSelf(preHandler);

        for (Batch<DraftSpi> batch : preHandler.batches()) {
            for (ImmutableProp prop : batch.shape().getItemMap().keySet()) {
                if (prop.isAssociation(TargetLevel.ENTITY) && !prop.isColumnDefinition()) {
                    savePostAssociation(prop, batch);
                }
            }
        }
    }

    private void savePreAssociation(ImmutableProp prop, Batch<DraftSpi> batch) {
        if (!batch.shape().getIdItems().isEmpty() && batch.shape().getItemMap().size() == 1) {
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
                ctx.to(prop).throwIllegalTargetIds(targetIds);
            }
            return;
        }
        Saver targetSaver = new Saver(ctx.to(prop));
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
        Saver targetSaver = new Saver(ctx.to(prop));
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

    }

    private void saveSelf(PreHandler preHandler) {

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
