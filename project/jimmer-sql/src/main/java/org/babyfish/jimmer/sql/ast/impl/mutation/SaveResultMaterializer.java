package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.EntitiesImpl;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherFactory;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;

import java.util.*;

class SaveResultMaterializer {

    private final SaveContext ctx;

    SaveResultMaterializer(SaveContext ctx) {
        this.ctx = ctx;
    }

    void materialize(List<DraftSpi> drafts, Iterable<Batch<DraftSpi>> batches) {
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
                SaveResultMaterializer::isPolymorphicResidualField :
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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
}
