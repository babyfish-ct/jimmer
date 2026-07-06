package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.ast.TypeMatchMode;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.*;

public class Saver {

    private final SaveContext ctx;

    public Saver(
            SaveOptions options,
            Connection con,
            ImmutableType type,
            Fetcher<?> fetcher
    ) {
        this(
                new SaveContext(
                        options,
                        con,
                        type,
                        fetcher
                )
        );
    }

    Saver(SaveContext ctx) {
        this.ctx = ctx;
    }

    @SuppressWarnings("unchecked")
    public <E> SimpleSaveResult<E> save(E entity) {
        ImmutableType immutableType = ImmutableType.get(entity.getClass());
        validateInstantiableSaveType(immutableType, ctx.options);
        MutationTrigger trigger = ctx.trigger;
        // single object save also call `produceList` because `fetch` may change draft
        E newEntity = (E) Internal.produceList(
                immutableType,
                Collections.singleton(entity),
                drafts -> {
                    saveAllImpl((List<DraftSpi>) drafts);
                },
                trigger == null ? null : trigger::prepareSubmit
        ).get(0);
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
        return saveAll(entities, true);
    }

    @SuppressWarnings("unchecked")
    <E> BatchSaveResult<E> saveAll(Collection<E> entities, boolean submitTrigger) {
        if (entities.isEmpty()) {
            return new BatchSaveResult<>(Collections.emptyMap(), Collections.emptyList());
        }
        ImmutableType immutableType = ImmutableType.get(entities.iterator().next().getClass());
        validateInstantiableSaveType(immutableType, ctx.options);
        MutationTrigger trigger = ctx.trigger;
        List<E> newEntities = (List<E>) Internal.produceList(
                immutableType,
                entities,
                drafts -> {
                    saveAllImpl((List<DraftSpi>) drafts);
                },
                trigger == null ? null : trigger::prepareSubmit
        );
        if (trigger != null && submitTrigger) {
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

    @SuppressWarnings("unchecked")
    <E> BatchSaveResult<E> saveAllJoinedInsert(Collection<E> entities) {
        if (entities.isEmpty()) {
            return new BatchSaveResult<>(Collections.emptyMap(), Collections.emptyList());
        }
        MutationTrigger trigger = ctx.trigger;
        List<E> newEntities = (List<E>) Internal.produceList(
                entities,
                base -> ((ImmutableSpi) base).__type(),
                drafts -> saveAllJoinedInsertImpl((List<DraftSpi>) drafts),
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

    private void saveAllJoinedInsertImpl(List<DraftSpi> drafts) {
        if (drafts.isEmpty()) {
            return;
        }
        ImmutableType rootType = ctx.path.getType();
        InheritanceInfo inheritanceInfo = rootType.getInheritanceInfo();
        if (inheritanceInfo == null || inheritanceInfo.getStrategy() != InheritanceType.JOINED) {
            throw new AssertionError("Internal bug: mixed joined insert must target a joined inheritance root");
        }
        Map<ImmutableType, List<DraftSpi>> groupMap = new LinkedHashMap<>();
        for (DraftSpi draft : drafts) {
            validateInstantiableSaveType(draft.__type(), ctx.options);
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
        List<SaveSelfResult> selfResults = saveJoinedInsertSelf(rootType, inheritanceInfo, drafts, operations);
        for (int i = 0; i < operations.size(); i++) {
            SaveOperation operation = operations.get(i);
            operation.saver.finishSave(operation, selfResults.get(i));
        }
    }

    static void validateInstantiableSaveType(ImmutableType type, SaveOptions options) {
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null) {
            return;
        }
        TypeMatchMode resolvedMode = TypeMatchModes.resolve(type, options.getTypeMatchMode(type));
        if (resolvedMode == TypeMatchMode.POLYMORPHIC && options.isTypeChangeAllowed(type)) {
            throw new IllegalArgumentException(
                    "Cannot save inheritance entity type \"" +
                            type +
                            "\" with " +
                            TypeMatchMode.POLYMORPHIC +
                            " type match mode because typeChangeAllowed is true"
            );
        }
        if (type.isInstantiable()) {
            return;
        }
        if (inheritanceInfo.getRootType() == type &&
                options.getMode() == SaveMode.UPDATE_ONLY &&
                !options.isTypeChangeAllowed(type) &&
                resolvedMode == TypeMatchMode.POLYMORPHIC) {
            return;
        }
        throw new IllegalArgumentException(
                "Cannot save inheritance entity type \"" +
                        type +
                        "\" because it is abstract; only UPDATE_ONLY with typeChangeAllowed=false and " +
                        "typeMatchMode=AUTO/POLYMORPHIC is allowed"
        );
    }

    private void saveAllImpl(List<DraftSpi> drafts) {

        // Comment for Users
        //
        // If you need to troubleshoot complex issues or
        // are interested in studying the mechanism of
        // save-command, adding a breakpoint to this method
        // is useful.
        //
        // However, save-command saves arbitrary-shaped
        // data structures, which may involve recursion.
        //
        // Once the current method is called recursively,
        // breakpoint debugging can become very difficult
        // because you won’t know which recursive level
        // will be interrupted. In this case, setting a
        // conditional breakpoint using `ctx.path` will
        // become very helpful.

        SaveOperation operation = prepareSave(drafts);
        SaveSelfResult selfResult = saveSelf(operation);
        finishSave(operation, selfResult);
    }

    SaveOperation prepareSave(List<DraftSpi> drafts) {
        if (!drafts.isEmpty() && !isIdOnlyAssociationReference(drafts)) {
            validateInstantiableSaveType(drafts.get(0).__type(), ctx.options);
        }

        for (ImmutableProp prop : ctx.path.getType().getProps().values()) {
            if (isVisitable(prop) && prop.isReference(TargetLevel.ENTITY) && prop.isColumnDefinition()) {
                savePreAssociation(prop, drafts);
            }
        }
        new MappedIdResolver(ctx).resolve(drafts);

        PreHandler preHandler = PreHandler.of(ctx);
        for (DraftSpi draft : drafts) {
            preHandler.add(draft);
        }
        return new SaveOperation(this, drafts, preHandler, isOwnerAcceptanceRequired(preHandler));
    }

    private void finishSave(SaveOperation operation, SaveSelfResult selfResult) {
        finishAssociations(operation, selfResult);
        new SaveResultMaterializer(ctx).materialize(
                SaveBatches.drafts(operation.drafts, selfResult.acceptedDrafts),
                SaveBatches.selfBatches(operation.preHandler, selfResult.acceptedDrafts)
        );
    }

    void finishAssociations(SaveOperation operation, SaveSelfResult selfResult) {
        PreHandler preHandler = operation.preHandler;

        for (Batch<DraftSpi> batch : SaveBatches.associationBatches(preHandler, selfResult.acceptedDrafts)) {
            for (ImmutableProp prop : batch.shape().getGetterMap().keySet()) {
                if (isVisitable(prop) && prop.isAssociation(TargetLevel.ENTITY)) {
                    if (ctx.options.getAssociatedMode(prop) == AssociatedSaveMode.VIOLENTLY_REPLACE) {
                        clearAssociations(batch.entities(), prop);
                    }
                    setBackReference(prop, batch);
                    savePostAssociation(prop, batch, selfResult.detach);
                }
            }
        }
    }

    private boolean isIdOnlyAssociationReference(List<DraftSpi> drafts) {
        ImmutableProp prop = ctx.path.getProp();
        if (prop == null || !ctx.options.isIdOnlyAsReference(prop)) {
            return false;
        }
        for (DraftSpi draft : drafts) {
            if (!draft.__isLoaded(draft.__type().getIdProp().getId())) {
                return false;
            }
            for (ImmutableProp draftProp : draft.__type().getProps().values()) {
                if (!draftProp.isId() && draft.__isLoaded(draftProp.getId())) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void setBackReference(ImmutableProp prop, Batch<DraftSpi> batch) {
        ImmutableProp backProp = prop.getMappedBy();
        if (backProp != null && backProp.isColumnDefinition()) {
            ImmutableType parentType = prop.getDeclaringType();
            PropId idPropId = batch.shape().getType().getIdProp().getId();
            PropId propId = prop.getId();
            PropId backPropId = backProp.getId();
            for (DraftSpi draft : batch.entities()) {
                if (draft.__isLoaded(idPropId)) {
                    Object idOnlyParent = ImmutableObjects.makeIdOnly(parentType, draft.__get(idPropId));
                    Object associated = draft.__get(propId);
                    if (associated instanceof Collection<?>) {
                        for (DraftSpi child : (List<DraftSpi>) associated) {
                            child.__set(backPropId, idOnlyParent);
                        }
                    } else if (associated instanceof DraftSpi) {
                        ((DraftSpi) associated).__set(backPropId, idOnlyParent);
                    }
                }
            }
        }
    }

    private void savePreAssociation(ImmutableProp prop, List<DraftSpi> drafts) {
        Saver targetSaver = new Saver(ctx.prop(prop));
        List<DraftSpi> targets = new ArrayList<>(drafts.size());
        PropId targetPropId = prop.getId();
        for (DraftSpi draft : drafts) {
            if (draft.__isLoaded(targetPropId)) {
                DraftSpi target = (DraftSpi) draft.__get(targetPropId);
                if (target != null) {
                    targets.add(target);
                } else if (!prop.isNullable() || prop.isInputNotNull()) {
                    targetSaver.ctx.throwNullTarget();
                }
            }
        }
        if (!targets.isEmpty()) {
            targetSaver.saveAllImpl(targets);
        }
    }

    @SuppressWarnings("unchecked")
    private void savePostAssociation(
            ImmutableProp prop,
            Batch<DraftSpi> batch,
            boolean detachOtherSiblings
    ) {
        Saver targetSaver = new Saver(ctx.prop(prop));

        if (isReadOnlyMiddleTable(prop)) {
            targetSaver.ctx.throwReadonlyMiddleTable();
        }
        if (prop.isRemote() && prop.getMappedBy() != null) {
            targetSaver.ctx.throwReversedRemoteAssociation();
        }
        if (prop.getSqlTemplate() instanceof JoinTemplate) {
            targetSaver.ctx.throwUnstructuredAssociation();
        }

        List<DraftSpi> targets = new ArrayList<>(batch.entities().size());
        PropId targetPropId = prop.getId();
        for (DraftSpi draft : batch.entities()) {
            Object value = draft.__get(targetPropId);
            if (value instanceof List<?>) {
                targets.addAll((List<DraftSpi>) value);
            } else if (value != null) {
                targets.add((DraftSpi) value);
            } else if (!prop.isNullable() || prop.isInputNotNull()) {
                targetSaver.ctx.throwNullTarget();
            }
        }
        if (!targets.isEmpty()) {
            targetSaver.saveAllImpl(targets);
        }

        updateAssociations(batch, prop, detachOtherSiblings);
    }

    private boolean isVisitable(ImmutableProp prop) {
        ImmutableProp backRef = ctx.backReferenceProp;
        return backRef == null || backRef != prop;
    }

    private SaveSelfResult saveSelf(SaveOperation operation) {
        return saveSelf(operation.preHandler, operation.ownerAcceptanceRequired);
    }

    private List<SaveSelfResult> saveJoinedInsertSelf(
            ImmutableType rootType,
            InheritanceInfo inheritanceInfo,
            List<DraftSpi> drafts,
            List<SaveOperation> operations
    ) {
        Set<DraftSpi> selfDrafts = Collections.newSetFromMap(new IdentityHashMap<>());
        List<SaveSelfResult> results = new ArrayList<>(operations.size());
        for (SaveOperation operation : operations) {
            boolean detach = false;
            if (operation.ownerAcceptanceRequired) {
                throw new AssertionError("Internal bug: INSERT_ONLY cannot require owner acceptance");
            }
            for (Batch<DraftSpi> batch : operation.preHandler.batches()) {
                if (batch.mode() != SaveMode.INSERT_ONLY) {
                    throw new AssertionError("Internal bug: mixed joined insert can only handle INSERT_ONLY batches");
                }
                if (isDetachRequired(batch.mode())) {
                    detach = true;
                }
                selfDrafts.addAll(batch.entities());
            }
            results.add(new SaveSelfResult(detach, null));
        }
        if (selfDrafts.isEmpty()) {
            return results;
        }
        insertJoinedRootStages(rootType, inheritanceInfo, drafts, selfDrafts);
        insertJoinedChildStages(rootType, drafts, selfDrafts);
        return results;
    }

    private void insertJoinedRootStages(
            ImmutableType rootType,
            InheritanceInfo inheritanceInfo,
            List<DraftSpi> drafts,
            Set<DraftSpi> selfDrafts
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Map<Shape, StageGroup> groupMap = new LinkedHashMap<>();
        for (DraftSpi draft : drafts) {
            if (!selfDrafts.contains(draft)) {
                continue;
            }
            Shape shape = Operator.joinedRootShape(sqlClient, rootType, draft);
            groupMap.computeIfAbsent(shape, it -> new StageGroup(rootType, shape)).entities.add(draft);
        }
        Operator operator = new Operator(ctx);
        for (StageGroup group : groupMap.values()) {
            operator.insertJoinedRoot(
                    SaveBatches.of(group.shape, group.entities, SaveMode.INSERT_ONLY, SaveMode.INSERT_ONLY),
                    inheritanceInfo
            );
        }
    }

    private void insertJoinedChildStages(
            ImmutableType rootType,
            List<DraftSpi> drafts,
            Set<DraftSpi> selfDrafts
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Map<StageKey, StageGroup> groupMap = new LinkedHashMap<>();
        for (DraftSpi draft : drafts) {
            if (!selfDrafts.contains(draft)) {
                continue;
            }
            ImmutableType previousTableType = rootType;
            for (ImmutableType tableType : Operator.joinedTableTypes(rootType, draft.__type())) {
                Shape shape = Operator.joinedStageShape(sqlClient, previousTableType, tableType, draft);
                StageKey key = new StageKey(tableType, shape);
                groupMap.computeIfAbsent(key, it -> new StageGroup(tableType, shape)).entities.add(draft);
                previousTableType = tableType;
            }
        }
        for (StageGroup group : groupMap.values()) {
            SaveContext stageCtx = new SaveContext(
                    ctx.options,
                    ctx.con,
                    group.tableType,
                    ctx.fetcher,
                    ctx.trigger,
                    ctx.affectedRowCountMap
            );
            new Operator(stageCtx).insertJoinedStage(
                    SaveBatches.of(group.shape, group.entities, SaveMode.INSERT_ONLY, SaveMode.INSERT_ONLY),
                    group.tableType
            );
        }
    }

    private SaveSelfResult saveSelf(PreHandler preHandler, boolean ownerAcceptanceRequired) {
        Operator operator = new Operator(ctx, ownerAcceptanceRequired);
        boolean detach = false;
        if (!ownerAcceptanceRequired) {
            for (Batch<DraftSpi> batch : preHandler.batches()) {
                if (isDetachRequired(batch.mode())) {
                    detach = true;
                }
                saveBatch(preHandler, operator, batch);
            }
            return new SaveSelfResult(detach, null);
        }

        Set<DraftSpi> acceptedDrafts = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Batch<DraftSpi> batch : preHandler.batches()) {
            if (isDetachRequired(batch.mode())) {
                detach = true;
            }
            Operator.MutationRows mutationRows = saveBatch(preHandler, operator, batch);
            if (mutationRows.acceptedDrafts != null) {
                acceptedDrafts.addAll(mutationRows.acceptedDrafts);
            } else {
                acceptedDrafts.addAll(batch.entities());
            }
        }
        return new SaveSelfResult(detach, acceptedDrafts);
    }

    private Operator.MutationRows saveBatch(PreHandler preHandler, Operator operator, Batch<DraftSpi> batch) {
        switch (batch.mode()) {
            case INSERT_ONLY:
                return operator.insert(batch);
            case INSERT_IF_ABSENT:
                return operator.upsert(batch, true);
            case UPDATE_ONLY:
                return operator.update(
                        preHandler.originalIdObjMap(),
                        preHandler.originalkeyObjMap(),
                        batch
                );
            default:
                return operator.upsert(batch, false);
        }
    }

    static boolean isDetachRequired(SaveMode mode) {
        return mode != SaveMode.INSERT_ONLY && mode != SaveMode.INSERT_IF_ABSENT;
    }

    private boolean isOwnerAcceptanceRequired(PreHandler preHandler) {
        if (!isJoinedTypeBranchTarget()) {
            return false;
        }
        if (ctx.trigger != null) {
            return isOwnerAcceptanceMode();
        }
        if (!isOwnerAcceptanceMode()) {
            return false;
        }
        return hasLoadedPostAssociation(preHandler);
    }

    private boolean hasLoadedPostAssociation(PreHandler preHandler) {
        for (Batch<DraftSpi> batch : preHandler.batches()) {
            for (DraftSpi draft : batch.entities()) {
                for (ImmutableProp prop : draft.__type().getProps().values()) {
                    if (isVisitable(prop) &&
                            prop.isAssociation(TargetLevel.ENTITY) &&
                            !prop.isColumnDefinition() &&
                            draft.__isLoaded(prop.getId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOwnerAcceptanceMode() {
        switch (ctx.options.getMode()) {
            case INSERT_IF_ABSENT:
                return true;
            case UPDATE_ONLY:
            case UPSERT:
            case NON_IDEMPOTENT_UPSERT:
                return true;
            default:
                return false;
        }
    }

    private boolean isJoinedTypeBranchTarget() {
        ImmutableType type = ctx.path.getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        return inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                inheritanceInfo.getRootType() != type;
    }

    private void clearAssociations(Collection<? extends ImmutableSpi> rows, ImmutableProp prop) {
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
                            ),
                            ctx.options.isDissociationLogicalDeleteEnabled()
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
        IdPairs.Retain noTargetIdPairs = new NoTargetEntityIdPairsImpl(rows);
        if (subOperator != null) {
            subOperator.disconnectExcept(noTargetIdPairs, true);
        }
        if (middleTableOperator != null) {
            middleTableOperator.disconnectExcept(noTargetIdPairs);
        }
    }

    private void updateAssociations(Batch<DraftSpi> batch, ImmutableProp prop, boolean detach) {
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
                            ),
                            ctx.options.isDissociationLogicalDeleteEnabled()
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
        IdPairs.Retain retainedIdPairs = IdPairs.retain(batch.entities(), prop);
        if (subOperator != null && detach && ctx.options.getAssociatedMode(prop) == AssociatedSaveMode.REPLACE) {
            subOperator.disconnectExcept(retainedIdPairs, true);
        }
        if (middleTableOperator != null) {
            if (detach) {
                switch (ctx.options.getAssociatedMode(prop)) {
                    case APPEND:
                    case VIOLENTLY_REPLACE:
                        middleTableOperator.append(retainedIdPairs);
                        break;
                    case UPDATE:
                    case MERGE:
                        middleTableOperator.merge(retainedIdPairs);
                        break;
                    default:
                        middleTableOperator.replace(retainedIdPairs);
                        break;
                }
            } else {
                middleTableOperator.append(retainedIdPairs);
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

    private static class StageKey {

        final ImmutableType tableType;

        final Shape shape;

        StageKey(ImmutableType tableType, Shape shape) {
            this.tableType = tableType;
            this.shape = shape;
        }

        @Override
        public int hashCode() {
            return tableType.hashCode() * 31 + shape.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof StageKey)) {
                return false;
            }
            StageKey other = (StageKey) obj;
            return tableType == other.tableType && shape.equals(other.shape);
        }
    }

    private static class StageGroup {

        final ImmutableType tableType;

        final Shape shape;

        final EntityList<DraftSpi> entities = new EntityList<>();

        StageGroup(ImmutableType tableType, Shape shape) {
            this.tableType = tableType;
            this.shape = shape;
        }
    }

}
