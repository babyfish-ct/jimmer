package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.*;

class JoinedMixedSave {

    private final SaveContext ctx;

    JoinedMixedSave(SaveContext ctx) {
        this.ctx = ctx;
    }

    @SuppressWarnings("unchecked")
    <E> BatchSaveResult<E> saveAll(Collection<E> entities) {
        if (entities.isEmpty()) {
            return new BatchSaveResult<>(Collections.emptyMap(), Collections.emptyList());
        }
        MutationTrigger trigger = ctx.trigger;
        List<E> newEntities = (List<E>) Internal.produceList(
                entities,
                base -> ((ImmutableSpi) base).__type(),
                drafts -> save((List<DraftSpi>) drafts),
                trigger == null ? null : trigger::prepareSubmit
        );
        if (trigger != null) {
            trigger.submit(ctx.options.getSqlClient(), ctx.con);
        }
        Iterator<E> oldItr = entities.iterator();
        Iterator<E> newItr = newEntities.iterator();
        List<BatchSaveResult.Item<E>> items = new ArrayList<>(entities.size());
        while (oldItr.hasNext() && newItr.hasNext()) {
            items.add(
                    new BatchSaveResult.Item<>(
                            oldItr.next(),
                            newItr.next()
                    )
            );
        }
        return new BatchSaveResult<>(ctx.affectedRowCountMap, items);
    }

    private void save(List<DraftSpi> drafts) {
        if (drafts.isEmpty()) {
            return;
        }
        ImmutableType rootType = ctx.path.getType();
        InheritanceInfo inheritanceInfo = rootType.getInheritanceInfo();
        if (inheritanceInfo == null || inheritanceInfo.getStrategy() != InheritanceType.JOINED) {
            throw new AssertionError("Internal bug: mixed joined save must target a joined inheritance root");
        }
        Map<ImmutableType, List<DraftSpi>> groupMap = new LinkedHashMap<>();
        for (DraftSpi draft : drafts) {
            Saver.validateInstantiableSaveType(draft.__type(), ctx.options);
            groupMap.computeIfAbsent(draft.__type(), it -> new ArrayList<>()).add(draft);
        }
        List<SaveOperation> operations = new ArrayList<>(groupMap.size());
        for (Map.Entry<ImmutableType, List<DraftSpi>> e : groupMap.entrySet()) {
            SaveContext groupCtx = new SaveContext(
                    ctx.options,
                    ctx.con,
                    e.getKey(),
                    ctx.fetcher,
                    ctx.trigger,
                    ctx.affectedRowCountMap
            );
            Saver groupSaver = new Saver(groupCtx);
            operations.add(groupSaver.prepareSave(e.getValue()));
        }
        List<SaveSelfResult> selfResults = saveSelf(rootType, inheritanceInfo, operations);
        for (int i = 0; i < operations.size(); i++) {
            SaveOperation operation = operations.get(i);
            operation.saver.finishAssociations(operation, selfResults.get(i));
        }
        Set<DraftSpi> acceptedDrafts = SaveBatches.acceptedDrafts(operations, selfResults);
        new SaveResultMaterializer(ctx).materialize(
                SaveBatches.drafts(drafts, acceptedDrafts),
                SaveBatches.selfBatches(operations, selfResults)
        );
    }

    private List<SaveSelfResult> saveSelf(
            ImmutableType rootType,
            InheritanceInfo inheritanceInfo,
            List<SaveOperation> operations
    ) {
        Map<RootStageKey, RootStageGroup> rootGroupMap = new LinkedHashMap<>();
        boolean detach = false;
        for (SaveOperation operation : operations) {
            for (Batch<DraftSpi> batch : operation.preHandler.batches()) {
                if (Saver.isDetachRequired(batch.mode())) {
                    detach = true;
                }
                DraftSpi sample = batch.entities().iterator().next();
                Shape rootShape = Operator.joinedRootShape(ctx.options.getSqlClient(), rootType, sample);
                RootStageKey key = new RootStageKey(rootShape, batch.mode(), batch.originalMode());
                rootGroupMap.computeIfAbsent(key, it -> new RootStageGroup(rootShape, batch.mode(), batch.originalMode()))
                        .entities
                        .addAll(batch.entities());
            }
        }
        Set<DraftSpi> acceptedDrafts = Collections.newSetFromMap(new IdentityHashMap<>());
        Operator rootOperator = new Operator(ctx);
        for (RootStageGroup group : rootGroupMap.values()) {
            int[] rowCounts = saveRootStage(rootOperator, group, inheritanceInfo);
            SaveBatches.collectAcceptedDrafts(acceptedDrafts, group.entities, rowCounts);
        }
        saveChildStages(rootType, inheritanceInfo, operations, acceptedDrafts);
        List<SaveSelfResult> results = new ArrayList<>(operations.size());
        for (SaveOperation operation : operations) {
            Set<DraftSpi> operationAcceptedDrafts = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Batch<DraftSpi> batch : operation.preHandler.batches()) {
                for (DraftSpi draft : batch.entities()) {
                    if (acceptedDrafts.contains(draft)) {
                        operationAcceptedDrafts.add(draft);
                    }
                }
            }
            results.add(new SaveSelfResult(detach, operationAcceptedDrafts));
        }
        return results;
    }

    private int[] saveRootStage(
            Operator rootOperator,
            RootStageGroup group,
            InheritanceInfo inheritanceInfo
    ) {
        Batch<DraftSpi> batch = SaveBatches.of(group.shape, group.entities, group.mode, group.originalMode);
        switch (group.mode) {
            case INSERT_ONLY:
                rootOperator.insertJoinedRoot(batch, inheritanceInfo);
                return SaveBatches.acceptedRowCounts(group.entities.size());
            case INSERT_IF_ABSENT:
                return rootOperator.upsertJoinedRootStage(batch, inheritanceInfo, true, false, false);
            case UPDATE_ONLY:
                return rootOperator.updateJoinedRootStage(null, null, batch, inheritanceInfo, false, false);
            default:
                return rootOperator.upsertJoinedRootStage(batch, inheritanceInfo, false, true, false);
        }
    }

    private void saveChildStages(
            ImmutableType rootType,
            InheritanceInfo inheritanceInfo,
            List<SaveOperation> operations,
            Set<DraftSpi> acceptedDrafts
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Map<ChildStageKey, ChildStageGroup> groupMap = new LinkedHashMap<>();
        for (SaveOperation operation : operations) {
            for (Batch<DraftSpi> batch : operation.preHandler.batches()) {
                for (DraftSpi draft : batch.entities()) {
                    if (!acceptedDrafts.contains(draft)) {
                        continue;
                    }
                    ImmutableType previousTableType = rootType;
                    for (ImmutableType tableType : Operator.joinedTableTypes(rootType, draft.__type())) {
                        Shape shape = Operator.joinedStageShape(sqlClient, previousTableType, tableType, draft);
                        ChildStageKey key = new ChildStageKey(tableType, shape, batch.mode(), batch.originalMode());
                        groupMap.computeIfAbsent(
                                key,
                                it -> new ChildStageGroup(tableType, shape, batch.mode(), batch.originalMode())
                        ).entities.add(draft);
                        previousTableType = tableType;
                    }
                }
            }
        }
        for (ChildStageGroup group : groupMap.values()) {
            SaveContext stageCtx = new SaveContext(
                    ctx.options,
                    ctx.con,
                    group.tableType,
                    ctx.fetcher,
                    ctx.trigger,
                    ctx.affectedRowCountMap
            );
            Operator operator = new Operator(stageCtx);
            Batch<DraftSpi> batch = SaveBatches.of(group.shape, group.entities, group.mode, group.originalMode);
            switch (group.mode) {
                case INSERT_ONLY:
                case INSERT_IF_ABSENT:
                    operator.insertJoinedStage(batch, group.tableType);
                    break;
                case UPDATE_ONLY:
                    operator.updateJoinedStage(batch, group.tableType, rootType, inheritanceInfo);
                    break;
                default:
                    operator.upsertJoinedStage(batch, group.tableType);
                    break;
            }
        }
    }

    private static class RootStageKey {

        final Shape shape;

        final SaveMode mode;

        final SaveMode originalMode;

        RootStageKey(Shape shape, SaveMode mode, SaveMode originalMode) {
            this.shape = shape;
            this.mode = mode;
            this.originalMode = originalMode;
        }

        @Override
        public int hashCode() {
            int hash = shape.hashCode();
            hash = hash * 31 + mode.hashCode();
            hash = hash * 31 + originalMode.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RootStageKey)) {
                return false;
            }
            RootStageKey other = (RootStageKey) obj;
            return shape.equals(other.shape) &&
                    mode == other.mode &&
                    originalMode == other.originalMode;
        }
    }

    private static class RootStageGroup {

        final Shape shape;

        final SaveMode mode;

        final SaveMode originalMode;

        final EntityList<DraftSpi> entities = new EntityList<>();

        RootStageGroup(Shape shape, SaveMode mode, SaveMode originalMode) {
            this.shape = shape;
            this.mode = mode;
            this.originalMode = originalMode;
        }
    }

    private static class ChildStageKey {

        final ImmutableType tableType;

        final Shape shape;

        final SaveMode mode;

        final SaveMode originalMode;

        ChildStageKey(ImmutableType tableType, Shape shape, SaveMode mode, SaveMode originalMode) {
            this.tableType = tableType;
            this.shape = shape;
            this.mode = mode;
            this.originalMode = originalMode;
        }

        @Override
        public int hashCode() {
            int hash = tableType.hashCode();
            hash = hash * 31 + shape.hashCode();
            hash = hash * 31 + mode.hashCode();
            hash = hash * 31 + originalMode.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ChildStageKey)) {
                return false;
            }
            ChildStageKey other = (ChildStageKey) obj;
            return tableType == other.tableType &&
                    shape.equals(other.shape) &&
                    mode == other.mode &&
                    originalMode == other.originalMode;
        }
    }

    private static class ChildStageGroup {

        final ImmutableType tableType;

        final Shape shape;

        final SaveMode mode;

        final SaveMode originalMode;

        final EntityList<DraftSpi> entities = new EntityList<>();

        ChildStageGroup(ImmutableType tableType, Shape shape, SaveMode mode, SaveMode originalMode) {
            this.tableType = tableType;
            this.shape = shape;
            this.mode = mode;
            this.originalMode = originalMode;
        }
    }
}
