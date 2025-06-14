package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.EntitiesImpl;
import org.babyfish.jimmer.sql.ast.impl.table.FetcherSelectionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherUtil;
import org.babyfish.jimmer.sql.meta.JoinTemplate;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;

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

    private Saver(SaveContext ctx) {
        this.ctx = ctx;
    }

    @SuppressWarnings("unchecked")
    public <E> SimpleSaveResult<E> save(E entity) {
        ImmutableType immutableType = ImmutableType.get(entity.getClass());
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
        if (entities.isEmpty()) {
            return new BatchSaveResult<>(Collections.emptyMap(), Collections.emptyList());
        }
        ImmutableType immutableType = ImmutableType.get(entities.iterator().next().getClass());
        MutationTrigger trigger = ctx.trigger;
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

        for (ImmutableProp prop : ctx.path.getType().getProps().values()) {
            if (isVisitable(prop) && prop.isReference(TargetLevel.ENTITY) && prop.isColumnDefinition()) {
                savePreAssociation(prop, drafts);
            }
        }

        PreHandler preHandler = PreHandler.of(ctx);
        for (DraftSpi draft : drafts) {
            preHandler.add(draft);
        }

        boolean detach = saveSelf(preHandler);

        for (Batch<DraftSpi> batch : preHandler.associationBatches()) {
            for (ImmutableProp prop : batch.shape().getGetterMap().keySet()) {
                if (isVisitable(prop) && prop.isAssociation(TargetLevel.ENTITY)) {
                    if (ctx.options.getAssociatedMode(prop) == AssociatedSaveMode.VIOLENTLY_REPLACE) {
                        clearAssociations(batch.entities(), prop);
                    }
                    setBackReference(prop, batch);
                    savePostAssociation(prop, batch, detach);
                }
            }
        }

        fetch(drafts, preHandler.batches());
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
        if (ctx.path.getBackProp() == null || ctx.path.getBackProp().isColumnDefinition()) {
            return;
        }
        boolean needFetch = false;
        for (Batch<DraftSpi> batch : batches) {
            if (batch.shape().getIdGetters().isEmpty()) {
                needFetch = true;
                break;
            }
        }
        if (!needFetch) {
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
        PropId idPropId = ctx.path.getType().getIdProp().getId();
        ShapeMatchContext shapeMatchContext = new ShapeMatchContext();
        Fetcher<?> fetcher = ctx.fetcher;
        for (DraftSpi draft : drafts) {
            if (!draft.__isLoaded(idPropId)) {
                nonIdObjects.add(draft);
            } else if (fetcher != null && !isShapeMatched(draft, fetcher, shapeMatchContext)) {
                arr[index] = draft;
                unmatchedIds.add(draft.__get(idPropId));
            }
            ++index;
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

    private boolean isVisitable(ImmutableProp prop) {
        ImmutableProp backRef = ctx.backReferenceProp;
        return backRef == null || backRef != prop;
    }

    private boolean saveSelf(PreHandler preHandler) {
        Operator operator = new Operator(ctx);
        boolean detach = false;
        for (Batch<DraftSpi> batch : preHandler.batches()) {
            switch (batch.mode()) {
                case INSERT_ONLY:
                    operator.insert(batch);
                    break;
                case INSERT_IF_ABSENT:
                    operator.upsert(batch, true);
                    break;
                case UPDATE_ONLY:
                    detach = true;
                    operator.update(
                            preHandler.originalIdObjMap(),
                            preHandler.originalkeyObjMap(),
                            batch
                    );
                    break;
                default:
                    detach = true;
                    operator.upsert(batch, false);
                    break;
            }
        }
        return detach;
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
        IdPairs.Retain retainedIdPairs = IdPairs.retain(batch.entities(), prop);
        if (subOperator != null && detach && ctx.options.getAssociatedMode(prop) == AssociatedSaveMode.REPLACE) {
            subOperator.disconnectExcept(retainedIdPairs, true);
        }
        if (middleTableOperator != null) {
            if (detach) {
                switch (ctx.options.getAssociatedMode(prop)) {
                    case APPEND:
                    case APPEND_IF_ABSENT:
                    case VIOLENTLY_REPLACE:
                        middleTableOperator.append(retainedIdPairs);
                        break;
                    case UPDATE:
                    case MERGE:
                        middleTableOperator.merge(retainedIdPairs);
                        break;
                    case REPLACE:
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

    @SuppressWarnings("unchecked")
    private static boolean isShapeMatched(DraftSpi draft, @Nullable Fetcher<?> fetcher, ShapeMatchContext ctx) {
        if (draft == null) {
            return true;
        }
        if (!ctx.isOptimizable(draft.__type(), fetcher)) {
            return false;
        }
        if (fetcher != null) {
            for (Field field : fetcher.getFieldMap().values()) {
                ImmutableProp prop = field.getProp();
                PropId propId = prop.getId();
                if (!draft.__isLoaded(propId)) {
                    return false;
                }
                if (prop.isAssociation(TargetLevel.ENTITY) || prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                    Fetcher<?> childFetcher = field.getChildFetcher();
                    Object associatedValue = draft.__get(propId);
                    if (prop.isReferenceList(TargetLevel.ENTITY)) {
                        List<DraftSpi> list = (List<DraftSpi>) associatedValue;
                        for (DraftSpi e : list) {
                            if (!isShapeMatched(e, childFetcher, ctx)) {
                                return false;
                            }
                        }
                    } else if (!isShapeMatched((DraftSpi) associatedValue, childFetcher, ctx)) {
                        return false;
                    }
                }
            }
            for (ImmutableProp prop : draft.__type().getProps().values()) {
                PropId propId = prop.getId();
                if (!draft.__isLoaded(propId)) {
                    continue;
                }
                Field field = fetcher.getFieldMap().get(prop.getName());
                if (field == null) {
                    draft.__unload(propId);
                } else if (field.isImplicit()) {
                    draft.__show(propId, false);
                }
            }
        } else {
            for (ImmutableProp prop : draft.__type().getProps().values()) {
                PropId propId = prop.getId();
                if (!draft.__isLoaded(propId)) {
                    return false;
                }
                if (prop.isAssociation(TargetLevel.ENTITY) || prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                    Object associatedValue = draft.__get(propId);
                    if (prop.isReferenceList(TargetLevel.ENTITY)) {
                        List<DraftSpi> list = (List<DraftSpi>) associatedValue;
                        for (DraftSpi e : list) {
                            if (!isShapeMatched(e, null, ctx)) {
                                return false;
                            }
                        }
                    } else if (!isShapeMatched((DraftSpi) associatedValue, null, ctx)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private class ShapeMatchContext {

        private final Map<Fetcher<?>, Boolean> optimizableMap = new HashMap<>();

        public boolean isOptimizable(ImmutableType type, @Nullable Fetcher<?> fetcher) {
            if (fetcher != null) {
                return optimizableMap.computeIfAbsent(fetcher, this::isOptimizableImpl);
            }
            return ctx.options.getUpsertMask(type) != null;
        }

        private boolean isOptimizableImpl(Fetcher<?> fetcher) {
            if (fetcher.getFieldMap().size() == 1 &&
                    fetcher.getFieldMap().values().iterator().next().getProp().isId()) {
                return true;
            }
            UpsertMask<?> mask = ctx.options.getUpsertMask(fetcher.getImmutableType());
            if (mask == null) {
                return true;
            }
            if (mask.getInsertablePaths() != null) {
                for (Field field : fetcher.getFieldMap().values()) {
                    ImmutableProp prop = field.getProp();
                    boolean matched = false;
                    for (List<ImmutableProp> path : mask.getInsertablePaths()) {
                        if (path.get(0) == prop) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        return false;
                    }
                }
            }
            if (mask.getUpdatablePaths() != null) {
                for (Field field : fetcher.getFieldMap().values()) {
                    ImmutableProp prop = field.getProp();
                    boolean matched = false;
                    for (List<ImmutableProp> path : mask.getUpdatablePaths()) {
                        if (path.get(0) == prop) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
