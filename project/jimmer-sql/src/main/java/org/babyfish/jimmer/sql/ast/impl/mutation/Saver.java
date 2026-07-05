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
        PreHandler preHandler = operation.preHandler;
        List<DraftSpi> drafts = operation.drafts;

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

        fetch(fetchDrafts(drafts, selfResult.acceptedDrafts), batches(preHandler, selfResult.acceptedDrafts));
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
        DraftSpi[] residualArr = new DraftSpi[drafts.size()];
        List<Object> residualIds = new ArrayList<>();
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
                !fillIdIfNecessary &&
                !fetcherAnalysis.hasTypeBranches() ?
                residualFetcher(fetcher) :
                null;
        PropId idPropId = ctx.path.getType().getIdProp().getId();
        SaveShapeMatcher shapeMatcher = new SaveShapeMatcher(ctx.options::getUpsertMask);
        for (DraftSpi draft : drafts) {
            if (!draft.__isLoaded(idPropId)) {
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
                    residualArr[index] = draft;
                    residualIds.add(id);
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
        if (!residualIds.isEmpty()) {
            JSqlClient sqlClient = ctx.options.getSqlClient().caches(CacheDisableConfig::disableAll);
            Map<Object, Object> map = ((EntitiesImpl) sqlClient.getEntities())
                    .forSaveCommandFetch(QueryReason.FETCHER)
                    .forConnection(ctx.con)
                    .findMapByIds(residualFetcher, residualIds);
            index = 0;
            for (DraftSpi draft : residualArr) {
                if (draft != null) {
                    Object id = draft.__get(idPropId);
                    Object fetched = map.get(id);
                    if (mergeDraft(draft, fetched) &&
                            shapeMatcher.isMatched(draft, fetcher, false)) {
                        shapeMatcher.trim(draft, fetcher);
                    } else {
                        arr[index] = draft;
                        unmatchedIds.add(id);
                    }
                }
                ++index;
            }
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
    private static Fetcher<Object> residualFetcher(Fetcher<?> fetcher) {
        Fetcher<Object> residualFetcher = (Fetcher<Object>) FetcherFactory.filter(
                (Fetcher<Object>) fetcher,
                null,
                (prop, path) -> !path.isEmpty() || !SaveFetcherAnalysis.isScalarColumnProp(prop)
        );
        return isIdOnlyFetcher(residualFetcher) ? null : residualFetcher;
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
