package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.TypeMatchMode;
import org.babyfish.jimmer.sql.ast.impl.EntitiesImpl;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherFactory;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

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

    @SuppressWarnings("unchecked")
    <E> BatchSaveResult<E> saveAllJoinedMixed(Collection<E> entities) {
        if (entities.isEmpty()) {
            return new BatchSaveResult<>(Collections.emptyMap(), Collections.emptyList());
        }
        MutationTrigger trigger = ctx.trigger;
        List<E> newEntities = (List<E>) Internal.produceList(
                entities,
                base -> ((ImmutableSpi) base).__type(),
                drafts -> saveAllJoinedMixedImpl((List<DraftSpi>) drafts),
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

    private void saveAllJoinedMixedImpl(List<DraftSpi> drafts) {
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
        List<SaveSelfResult> selfResults = saveJoinedMixedSelf(rootType, inheritanceInfo, operations);
        for (int i = 0; i < operations.size(); i++) {
            SaveOperation operation = operations.get(i);
            operation.saver.finishAssociations(operation, selfResults.get(i));
        }
        Set<DraftSpi> acceptedDrafts = acceptedDrafts(operations, selfResults);
        fetch(
                fetchDrafts(drafts, acceptedDrafts),
                batches(operations, selfResults)
        );
    }

    private List<SaveSelfResult> saveJoinedMixedSelf(
            ImmutableType rootType,
            InheritanceInfo inheritanceInfo,
            List<SaveOperation> operations
    ) {
        Map<RootStageKey, RootStageGroup> rootGroupMap = new LinkedHashMap<>();
        boolean detach = false;
        for (SaveOperation operation : operations) {
            for (Batch<DraftSpi> batch : operation.preHandler.batches()) {
                if (isDetachRequired(batch.mode())) {
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
            int[] rowCounts = saveJoinedRootStage(rootOperator, group, inheritanceInfo);
            collectAcceptedDrafts(acceptedDrafts, group.entities, rowCounts);
        }
        saveJoinedChildStages(rootType, inheritanceInfo, operations, acceptedDrafts);
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

    private int[] saveJoinedRootStage(
            Operator rootOperator,
            RootStageGroup group,
            InheritanceInfo inheritanceInfo
    ) {
        Batch<DraftSpi> batch = batchOf(group.shape, group.entities, group.mode, group.originalMode);
        switch (group.mode) {
            case INSERT_ONLY:
                rootOperator.insertJoinedRoot(batch, inheritanceInfo);
                return acceptedRowCounts(group.entities.size());
            case INSERT_IF_ABSENT:
                return rootOperator.upsertJoinedRootStage(batch, inheritanceInfo, true, false, false);
            case UPDATE_ONLY:
                return rootOperator.updateJoinedRootStage(null, null, batch, inheritanceInfo, false, false);
            default:
                return rootOperator.upsertJoinedRootStage(batch, inheritanceInfo, false, true, false);
        }
    }

    private void saveJoinedChildStages(
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
            Batch<DraftSpi> batch = batchOf(group.shape, group.entities, group.mode, group.originalMode);
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

    private SaveOperation prepareSave(List<DraftSpi> drafts) {
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
        fetch(
                fetchDrafts(operation.drafts, selfResult.acceptedDrafts),
                batches(operation.preHandler, selfResult.acceptedDrafts)
        );
    }

    private void finishAssociations(SaveOperation operation, SaveSelfResult selfResult) {
        PreHandler preHandler = operation.preHandler;

        for (Batch<DraftSpi> batch : associationBatches(preHandler, selfResult.acceptedDrafts)) {
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

    private void fetch(List<DraftSpi> drafts, Iterable<Batch<DraftSpi>> batches) {
        if (ctx.path.getParent() != null) {
            fetchIdIfNecessary(drafts, batches);
            return;
        }
        Fetcher<?> fetcher = ctx.fetcher;
        if (fetcher == null) {
            fetchIdIfNecessary(drafts, batches);
            return;
        }
        fetchImpl(drafts, batches, false);
    }

    private void fetchIdIfNecessary(List<DraftSpi> drafts, Iterable<Batch<DraftSpi>> batches) {
        if (ctx.options.getMode() != SaveMode.INSERT_IF_ABSENT) {
            return;
        }
        if (!ctx.isIdRetrievingRequired()) {
            return;
        }
        fetchImpl(drafts, batches, true);
    }

    @SuppressWarnings("unchecked")
    private void fetchImpl(
            List<DraftSpi> drafts,
            Iterable<Batch<DraftSpi>> batches,
            boolean fillIdIfNecessary
    ) {
        DraftSpi[] arr = new DraftSpi[drafts.size()];
        int index = 0;
        List<Object> unmatchedIds = new ArrayList<>();
        List<DraftSpi> nonIdObjects = new ArrayList<>();
        Fetcher<?> fetcher = ctx.fetcher;
        ResidualFetchGroup residualGroup = null;
        DraftSpi[] databaseDefaultArr = new DraftSpi[drafts.size()];
        List<Object> databaseDefaultIds = new ArrayList<>();
        SaveFetcherAnalysis fetcherAnalysis = fetcher != null ?
                SaveFetcherAnalysis.of(fetcher) :
                null;
        List<ImmutableProp> databaseDefaultProps =
                fetcherAnalysis != null && fetcherAnalysis.isScalarOnly() ?
                        fetcherAnalysis.getDatabaseDefaultProps() :
                        Collections.emptyList();
        boolean canFetchDatabaseDefaults = fetcherAnalysis != null && !databaseDefaultProps.isEmpty();
        Fetcher<Object> residualFetcher = fetcher != null &&
                fetcherAnalysis != null &&
                !fillIdIfNecessary ?
                residualFetcher(fetcher) :
                null;
        if (residualFetcher != null) {
            residualGroup = new ResidualFetchGroup(residualFetcher);
        }
        PropId idPropId = ctx.path.getType().getIdProp().getId();
        SaveShapeMatcher shapeMatcher = new SaveShapeMatcher(ctx.options::getUpsertMask);
        for (DraftSpi draft : drafts) {
            if (ctx.isSaveReturningNotAccepted(draft)) {
                // Returning row-count 0 rows must remain unmaterialized.
                ++index;
                continue;
            } else if (!draft.__isLoaded(idPropId)) {
                nonIdObjects.add(draft);
            } else if (fetcher != null && !shapeMatcher.isMatched(draft, fetcher, true)) {
                Object id = draft.__get(idPropId);
                if (canFetchDatabaseDefaults &&
                        fetcherAnalysis.isUnmatchedOnlyByDatabaseDefaultProps(draft)) {
                    databaseDefaultArr[index] = draft;
                    databaseDefaultIds.add(id);
                } else if (residualFetcher != null &&
                        ctx.isSaveReturningApplied(draft) &&
                        fetcherAnalysis.areReturningPropsLoaded(draft)) {
                    residualGroup.add(index, draft, id);
                } else {
                    arr[index] = draft;
                    unmatchedIds.add(id);
                }
            }
            ++index;
        }
        if (!databaseDefaultIds.isEmpty()) {
            JSqlClient sqlClient = ctx.options.getSqlClient().caches(CacheDisableConfig::disableAll);
            Fetcher<ImmutableSpi> databaseDefaultFetcher =
                    databaseDefaultFetcher(databaseDefaultProps);
            Map<Object, ImmutableSpi> map = ((EntitiesImpl) sqlClient.getEntities())
                    .forSaveCommandFetch(QueryReason.FETCHER)
                    .forConnection(ctx.con)
                    .findMapByIds(databaseDefaultFetcher, databaseDefaultIds);
            index = 0;
            for (DraftSpi draft : databaseDefaultArr) {
                if (draft != null) {
                    Object id = draft.__get(idPropId);
                    ImmutableSpi fetched = map.get(id);
                    if (fetched != null) {
                        applyDatabaseDefaultProps(draft, fetched, databaseDefaultProps);
                    } else {
                        arr[index] = draft;
                        unmatchedIds.add(id);
                    }
                }
                ++index;
            }
        }
        if (residualGroup != null && !residualGroup.ids.isEmpty()) {
            fetchResidual(residualGroup, arr, unmatchedIds, shapeMatcher, fetcher, idPropId);
        }
        if (!unmatchedIds.isEmpty()) {
            JSqlClient sqlClient = ctx.options.getSqlClient().caches(CacheDisableConfig::disableAll);
            Map<Object, Object> map = ((EntitiesImpl) sqlClient.getEntities())
                    .forSaveCommandFetch(fillIdIfNecessary ? QueryReason.GET_ID_FOR_PRE_SAVED_ENTITIES : QueryReason.FETCHER)
                    .forConnection(ctx.con)
                    .findMapByIds(
                            fillIdIfNecessary ?
                                    new FetcherImpl<>((Class<Object>) ctx.path.getType().getJavaClass()) :
                                    (Fetcher<Object>) fetcher,
                            unmatchedIds
                    );
            index = 0;
            ListIterator<DraftSpi> itr = drafts.listIterator();
            while (itr.hasNext()) {
                DraftSpi draft = itr.next();
                if (arr[index] != null) {
                    Object fetched = map.get(draft.__get(idPropId));
                    DraftSpi replacedDraft = replaceDraft(draft, fetched);
                    if (replacedDraft != null) {
                        itr.set(replacedDraft);
                    }
                }
                ++index;
            }
        }
        if (!nonIdObjects.isEmpty()) {
            if (drafts.size() == 1) {
                Map<KeyMatcher.Group, List<ImmutableSpi>> rowMap = Rows.findByKeys(
                        ctx,
                        fillIdIfNecessary ? QueryReason.GET_ID_FOR_PRE_SAVED_ENTITIES : QueryReason.FETCHER,
                        fillIdIfNecessary ?
                                new FetcherImpl<>((Class<ImmutableSpi>) ctx.path.getType().getJavaClass()) :
                                (Fetcher<ImmutableSpi>) fetcher,
                        nonIdObjects,
                        null
                );
                if (!rowMap.isEmpty()) {
                    ImmutableSpi row = rowMap.values().iterator().next().iterator().next();
                    if (fillIdIfNecessary) {
                        drafts.get(0).__set(idPropId, row.__get(idPropId));
                    } else {
                        ListIterator<DraftSpi> itr = drafts.listIterator();
                        DraftSpi draft = itr.next();
                        DraftSpi replaceDraft = replaceDraft(draft, row);
                        if (replaceDraft != null) {
                            itr.set(replaceDraft);
                        }
                    }
                }
            } else {
                KeyMatcher keyMatcher = ctx.options.getKeyMatcher(ctx.path.getType());
                for (Batch<DraftSpi> batch : batches) {
                    Set<ImmutableProp> keyProps = batch.shape().keyProps(keyMatcher);
                    List<PropId> unloadPropIds = null;
                    if (!fillIdIfNecessary) {
                        assert fetcher != null;
                        unloadPropIds = new ArrayList<>();
                        for (ImmutableProp keyProp : keyProps) {
                            if (!((FetcherImplementor<?>) fetcher).__contains(keyProp.getName())) {
                                fetcher = fetcher.add(keyProp.getName());
                                unloadPropIds.add(keyProp.getId());
                            }
                        }
                    }
                    Fetcher<ImmutableSpi> actualFetcher;
                    if (fillIdIfNecessary) {
                        actualFetcher = new FetcherImpl<>((Class<ImmutableSpi>) ctx.path.getType().getJavaClass());
                        for (ImmutableProp keyProp : keyProps) {
                            actualFetcher = actualFetcher.add(keyProp.getName());
                        }
                    } else {
                        actualFetcher = (Fetcher<ImmutableSpi>) fetcher;
                    }
                    Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> map = Rows.findMapByKeys(
                            ctx,
                            fillIdIfNecessary ? QueryReason.GET_ID_FOR_PRE_SAVED_ENTITIES : QueryReason.FETCHER,
                            actualFetcher,
                            nonIdObjects
                    );
                    if (map.isEmpty()) {
                        continue;
                    }
                    ListIterator<DraftSpi> itr = drafts.listIterator();
                    while (itr.hasNext()) {
                        DraftSpi draft = itr.next();
                        if (draft.__isLoaded(idPropId)) {
                            continue;
                        }
                        Map<Object, ImmutableSpi> subMap = map.values().iterator().next();
                        Object key = Keys.keyOf(draft, keyProps);
                        ImmutableSpi fetched = subMap.get(key);
                        if (unloadPropIds == null) {
                            draft.__set(idPropId, fetched.__get(idPropId));
                        } else {
                            DraftSpi newDraft = replaceDraft(draft, fetched);
                            if (newDraft != null) {
                                itr.set(newDraft);
                            } else {
                                newDraft = draft;
                            }
                            for (PropId unloadedPropId : unloadPropIds) {
                                newDraft.__unload(unloadedPropId);
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchResidual(
            ResidualFetchGroup group,
            DraftSpi[] arr,
            List<Object> unmatchedIds,
            SaveShapeMatcher shapeMatcher,
            Fetcher<?> fetcher,
            PropId idPropId
    ) {
        JSqlClient sqlClient = ctx.options.getSqlClient().caches(CacheDisableConfig::disableAll);
        Fetcher<Object> residualFetcher = residualFetcher(group.fetcher, group.types);
        if (residualFetcher == null) {
            return;
        }
        Map<Object, Object> map = ((EntitiesImpl) sqlClient.getEntities())
                .forSaveCommandFetch(QueryReason.FETCHER)
                .forConnection(ctx.con)
                .findMapByIds(residualFetcher, group.ids);
        for (int i = 0; i < group.drafts.size(); i++) {
            DraftSpi draft = group.drafts.get(i);
            Object id = draft.__get(idPropId);
            Object fetched = map.get(id);
            if (mergeDraft(draft, fetched) &&
                    shapeMatcher.isMatched(draft, fetcher, false)) {
                shapeMatcher.trim(draft, fetcher);
            } else {
                arr[group.indexes.get(i)] = draft;
                unmatchedIds.add(id);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Fetcher<Object> residualFetcher(Fetcher<?> fetcher) {
        return residualFetcher(fetcher, Collections.emptySet());
    }

    @SuppressWarnings("unchecked")
    private static Fetcher<Object> residualFetcher(Fetcher<?> fetcher, Collection<ImmutableType> types) {
        boolean hasTypeBranches = !((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().isEmpty();
        ImmutableType rootType = fetcher.getImmutableType();
        FetcherFactory.PropFilter propFilter = hasTypeBranches ?
                Saver::isPolymorphicResidualField :
                (type, prop, path) -> !path.isEmpty() || !SaveFetcherAnalysis.isScalarColumnProp(prop);
        Fetcher<Object> residualFetcher = (Fetcher<Object>) FetcherFactory.filterByTypedProp(
                (Fetcher<Object>) fetcher,
                hasTypeBranches && !types.isEmpty() ?
                        (type, path) -> type == rootType || containsAssignableType(types, type) :
                        null,
                propFilter
        );
        return isIdOnlyFetcher(residualFetcher) ? null : residualFetcher;
    }

    private static boolean containsAssignableType(Collection<ImmutableType> types, ImmutableType type) {
        for (ImmutableType actualType : types) {
            if (type.isAssignableFrom(actualType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPolymorphicResidualField(
            ImmutableType type,
            ImmutableProp prop,
            List<ImmutableProp> path
    ) {
        if (!path.isEmpty() || !SaveFetcherAnalysis.isScalarColumnProp(prop)) {
            return true;
        }
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        return inheritanceInfo != null && !inheritanceInfo.isPropAvailableInTable(prop, inheritanceInfo.getRootType());
    }

    private static boolean isIdOnlyFetcher(Fetcher<?> fetcher) {
        if (!((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().isEmpty()) {
            return false;
        }
        Map<String, Field> fieldMap = fetcher.getFieldMap();
        return fieldMap.size() == 1 && fieldMap.values().iterator().next().getProp().isId();
    }

    private Fetcher<ImmutableSpi> databaseDefaultFetcher(List<ImmutableProp> props) {
        Fetcher<ImmutableSpi> fetcher = new FetcherImpl<>((Class<ImmutableSpi>) ctx.path.getType().getJavaClass());
        for (ImmutableProp prop : props) {
            fetcher = fetcher.add(prop.getName());
        }
        return fetcher;
    }

    private static void applyDatabaseDefaultProps(
            DraftSpi draft,
            ImmutableSpi fetched,
            List<ImmutableProp> props
    ) {
        for (ImmutableProp prop : props) {
            PropId propId = prop.getId();
            if (fetched.__isLoaded(propId)) {
                draft.__set(propId, fetched.__get(propId));
                draft.__show(propId, fetched.__isVisible(propId));
            }
        }
    }

    private static DraftSpi replaceDraft(DraftSpi draft, Object fetched) {
        if (fetched instanceof DraftSpi) {
            return (DraftSpi) fetched;
        }
        if (fetched instanceof ImmutableSpi) {
            ImmutableSpi spi = (ImmutableSpi) fetched;
            for (ImmutableProp prop : draft.__type().getProps().values()) {
                PropId propId = prop.getId();
                if (spi.__isLoaded(propId)) {
                    if (!prop.isView()) {
                        draft.__set(propId, spi.__get(propId));
                    }
                    draft.__show(propId, spi.__isVisible(propId));
                } else {
                    draft.__unload(propId);
                }
            }
        }
        return null;
    }

    private static boolean mergeDraft(DraftSpi draft, Object fetched) {
        if (fetched instanceof ImmutableSpi) {
            ImmutableSpi spi = (ImmutableSpi) fetched;
            for (ImmutableProp prop : draft.__type().getProps().values()) {
                PropId propId = prop.getId();
                if (spi.__isLoaded(propId)) {
                    if (!prop.isView()) {
                        Object value = spi.__get(propId);
                        if (value != null) {
                            if (prop.isReferenceList(TargetLevel.OBJECT)) {
                                value = draft.__draftContext().toDraftList(
                                        (List<Object>) value,
                                        (Class<Object>) prop.getElementClass(),
                                        true
                                );
                            } else if (prop.isReference(TargetLevel.OBJECT)) {
                                value = draft.__draftContext().toDraftObject(value);
                            }
                        }
                        draft.__set(propId, value);
                    }
                    draft.__show(propId, spi.__isVisible(propId));
                }
            }
            return true;
        }
        return false;
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
                    batchOf(group.shape, group.entities, SaveMode.INSERT_ONLY, SaveMode.INSERT_ONLY),
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
                    batchOf(group.shape, group.entities, SaveMode.INSERT_ONLY, SaveMode.INSERT_ONLY),
                    group.tableType
            );
        }
    }

    private static Batch<DraftSpi> batchOf(
            Shape shape,
            EntityCollection<DraftSpi> entities,
            SaveMode mode,
            SaveMode originalMode
    ) {
        return new Batch<DraftSpi>() {

            @Override
            public Shape shape() {
                return shape;
            }

            @Override
            public EntityCollection<DraftSpi> entities() {
                return entities;
            }

            @Override
            public SaveMode mode() {
                return mode;
            }

            @Override
            public SaveMode originalMode() {
                return originalMode;
            }
        };
    }

    private static int[] acceptedRowCounts(int size) {
        int[] rowCounts = new int[size];
        Arrays.fill(rowCounts, 1);
        return rowCounts;
    }

    private static void collectAcceptedDrafts(
            Set<DraftSpi> output,
            EntityCollection<DraftSpi> entities,
            int[] rowCounts
    ) {
        int index = 0;
        for (EntityCollection.Item<DraftSpi> item : entities.items()) {
            if (index < rowCounts.length && rowCounts[index++] != 0) {
                output.add(item.getEntity());
            }
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

    private static boolean isDetachRequired(SaveMode mode) {
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

    private Iterable<Batch<DraftSpi>> associationBatches(
            PreHandler preHandler,
            @Nullable Set<DraftSpi> acceptedDrafts
    ) {
        return acceptedDrafts != null ?
                filterBatches(preHandler.associationBatches(), acceptedDrafts) :
                preHandler.associationBatches();
    }

    private Iterable<Batch<DraftSpi>> batches(
            PreHandler preHandler,
            @Nullable Set<DraftSpi> acceptedDrafts
    ) {
        return acceptedDrafts != null ?
                filterBatches(preHandler.batches(), acceptedDrafts) :
                preHandler.batches();
    }

    private Iterable<Batch<DraftSpi>> batches(
            List<SaveOperation> operations,
            List<SaveSelfResult> selfResults
    ) {
        List<Batch<DraftSpi>> batches = new ArrayList<>();
        for (int i = 0; i < operations.size(); i++) {
            SaveOperation operation = operations.get(i);
            SaveSelfResult selfResult = selfResults.get(i);
            for (Batch<DraftSpi> batch : batches(operation.preHandler, selfResult.acceptedDrafts)) {
                batches.add(batch);
            }
        }
        return batches;
    }

    private static Set<DraftSpi> acceptedDrafts(
            List<SaveOperation> operations,
            List<SaveSelfResult> selfResults
    ) {
        Set<DraftSpi> acceptedDrafts = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < operations.size(); i++) {
            SaveOperation operation = operations.get(i);
            Set<DraftSpi> operationAcceptedDrafts = selfResults.get(i).acceptedDrafts;
            if (operationAcceptedDrafts != null) {
                acceptedDrafts.addAll(operationAcceptedDrafts);
            } else {
                acceptedDrafts.addAll(operation.drafts);
            }
        }
        return acceptedDrafts;
    }

    private List<DraftSpi> fetchDrafts(List<DraftSpi> drafts, @Nullable Set<DraftSpi> acceptedDrafts) {
        if (acceptedDrafts == null) {
            return drafts;
        }
        List<DraftSpi> filteredDrafts = new ArrayList<>(acceptedDrafts.size());
        for (DraftSpi draft : drafts) {
            if (acceptedDrafts.contains(draft)) {
                filteredDrafts.add(draft);
            }
        }
        return filteredDrafts;
    }

    private Iterable<Batch<DraftSpi>> filterBatches(
            Iterable<Batch<DraftSpi>> batches,
            Set<DraftSpi> acceptedDrafts
    ) {
        List<Batch<DraftSpi>> filteredBatches = new ArrayList<>();
        for (Batch<DraftSpi> batch : batches) {
            EntityList<DraftSpi> entities = new EntityList<>();
            for (DraftSpi draft : batch.entities()) {
                if (acceptedDrafts.contains(draft)) {
                    entities.add(draft);
                }
            }
            if (!entities.isEmpty()) {
                filteredBatches.add(new FilteredBatch(batch, entities));
            }
        }
        return filteredBatches;
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

    private static class SaveOperation {

        final Saver saver;

        final List<DraftSpi> drafts;

        final PreHandler preHandler;

        final boolean ownerAcceptanceRequired;

        SaveOperation(
                Saver saver,
                List<DraftSpi> drafts,
                PreHandler preHandler,
                boolean ownerAcceptanceRequired
        ) {
            this.saver = saver;
            this.drafts = drafts;
            this.preHandler = preHandler;
            this.ownerAcceptanceRequired = ownerAcceptanceRequired;
        }
    }

    private static class SaveSelfResult {

        final boolean detach;

        @Nullable
        final Set<DraftSpi> acceptedDrafts;

        SaveSelfResult(boolean detach, @Nullable Set<DraftSpi> acceptedDrafts) {
            this.detach = detach;
            this.acceptedDrafts = acceptedDrafts;
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

    private static class ResidualFetchGroup {

        final Fetcher<Object> fetcher;

        final List<Integer> indexes = new ArrayList<>();

        final List<DraftSpi> drafts = new ArrayList<>();

        final List<Object> ids = new ArrayList<>();

        final Set<ImmutableType> types = new LinkedHashSet<>();

        ResidualFetchGroup(Fetcher<Object> fetcher) {
            this.fetcher = fetcher;
        }

        void add(int index, DraftSpi draft, Object id) {
            indexes.add(index);
            drafts.add(draft);
            ids.add(id);
            types.add(draft.__type());
        }
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

    private static class FilteredBatch implements Batch<DraftSpi> {

        private final Batch<DraftSpi> base;

        private final EntityCollection<DraftSpi> entities;

        private FilteredBatch(Batch<DraftSpi> base, EntityCollection<DraftSpi> entities) {
            this.base = base;
            this.entities = entities;
        }

        @Override
        public Shape shape() {
            return base.shape();
        }

        @Override
        public EntityCollection<DraftSpi> entities() {
            return entities;
        }

        @Override
        public SaveMode mode() {
            return base.mode();
        }

        @Override
        public SaveMode originalMode() {
            return base.originalMode();
        }
    }

}
