package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.impl.util.CollectionUtils;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.EntitiesImpl;
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MergedTypedRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.impl.table.FetcherSelectionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheAbandonedCallback;
import org.babyfish.jimmer.sql.cache.CacheEnvironment;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;
import org.babyfish.jimmer.sql.fetcher.impl.*;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractDataLoader {

    /* For globalFilter is not null but not `Filter.Parameterized` */
    private static final SortedMap<String, Object> ILLEGAL_PARAMETERS =
            Collections.unmodifiableSortedMap(new TreeMap<>());

    private final JSqlClientImplementor sqlClient;

    private final Connection con;

    private final FetchPath path;

    private final ImmutableProp prop;

    private final Storage storage;

    private final boolean remote;

    private final ImmutableProp sourceIdProp;

    private final ImmutableProp targetIdProp;

    private final org.babyfish.jimmer.sql.filter.Filter<Props> globalFiler;

    private final FieldFilter<Table<ImmutableSpi>> propFilter;

    private final int limit;

    private final long offset;

    private final boolean rawValue;

    private final TransientResolver<?, ?> resolver;

    private final FetcherImplementor<ImmutableSpi> fetcher;

    @SuppressWarnings("unchecked")
    protected AbstractDataLoader(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType entityType,
            FetchPath path,
            ImmutableProp prop,
            Fetcher<?> fetcher,
            RecursionStrategy<?> parentRecursionStrategy,
            FieldFilter<?> propFilter,
            int limit,
            int offset,
            boolean rawValue
    ) {
        if (!prop.getDependencies().isEmpty()) {
            throw new IllegalArgumentException(
                    "\"" + prop + "\" is view(@IdView, @ManyToManyView, @Formula) based on other properties"
            );
        }
        if (!prop.isAssociation(TargetLevel.ENTITY) && !prop.hasTransientResolver()) {
            throw new IllegalArgumentException(
                    "\"" + prop + "\" is neither association nor transient with resolver"
            );
        }
        if ((limit != Integer.MAX_VALUE || offset != 0) && !prop.isReferenceList(TargetLevel.PERSISTENT)) {
            throw new IllegalArgumentException(
                    "Cannot specify association property \"" +
                            prop +
                            "\" because it not list association(one-to-many/many-to-many)"
            );
        }
        if (!prop.isAssociation(TargetLevel.ENTITY)) {
            if (fetcher != null) {
                throw new IllegalArgumentException(
                        "Cannot specify fetcher for scalar prop \"" +
                                prop +
                                "\""
                );
            }
            if (propFilter != null) {
                throw new IllegalArgumentException(
                        "Cannot specify filter for scalar prop \"" +
                                prop +
                                "\""
                );
            }
            if (limit != Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Cannot specify limit for scalar prop \"" +
                                prop +
                                "\""
                );
            }
            if (offset != 0) {
                throw new IllegalArgumentException(
                        "Cannot specify limit for scalar prop \"" +
                                prop +
                                "\""
                );
            }
        }
        if (entityType != null) {
            if (!entityType.isEntity()) {
                throw new IllegalArgumentException(
                        "\"" + entityType + "\" is not entity"
                );
            }
        } else if (!prop.getDeclaringType().isEntity()) {
            throw new IllegalArgumentException(
                    "\"" + prop + "\" is not declared in entity"
            );
        }
        this.sqlClient = sqlClient;
        this.con = con;
        this.path = FetchPath.of(path, prop);
        this.prop = prop;
        this.storage = prop.getStorage(sqlClient.getMetadataStrategy());
        this.remote = prop.isRemote();
        this.sourceIdProp = prop.getDeclaringType().getIdProp();
        this.targetIdProp = prop.getTargetType() != null ? prop.getTargetType().getIdProp() : null;
        if (prop.isAssociation(TargetLevel.PERSISTENT)) {
            globalFiler = sqlClient.getFilters().getTargetFilter(prop);
        } else {
            globalFiler = null;
        }
        this.propFilter = (FieldFilter<Table<ImmutableSpi>>) propFilter;
        if (propFilter != null && prop.isReference(TargetLevel.ENTITY) && !prop.isNullable()) {
            throw new ExecutionException(
                    "Cannot apply field filter of object fetcher for \"" +
                            prop +
                            "\" because that property is not nullable"
            );
        }
        this.limit = limit;
        this.offset = offset;
        this.rawValue = rawValue;
        if (prop.isAssociation(TargetLevel.PERSISTENT)) {
            this.resolver = null;
            this.fetcher = fetcher != null ?
                    (FetcherImplementor<ImmutableSpi>) fetcher :
                    new FetcherImpl<>((Class<ImmutableSpi>) prop.getTargetType().getJavaClass());
        } else {
            this.resolver = sqlClient.getResolver(prop);
            if (prop.isAssociation(TargetLevel.ENTITY)) {
                this.fetcher = fetcher != null ?
                        (FetcherImplementor<ImmutableSpi>) fetcher :
                        new FetcherImpl<>((Class<ImmutableSpi>) prop.getTargetType().getJavaClass());
            } else {
                this.fetcher = null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Map<ImmutableSpi, Object> load(Collection<ImmutableSpi> sources) {
        if (sources.isEmpty()) {
            return Collections.emptyMap();
        }
        if (resolver != null) {
            return loadTransients(sources);
        }
        if (storage instanceof ColumnDefinition) {
            return (Map<ImmutableSpi, Object>)(Map<?, ?>) loadParents(sources);
        }
        if (prop.isReferenceList(TargetLevel.ENTITY)) {
            return (Map<ImmutableSpi, Object>)(Map<?, ?>) loadTargetMultiMap(sources);
        }
        return (Map<ImmutableSpi, Object>)(Map<?, ?>) loadTargetMap(sources);
    }

    @SuppressWarnings("unchecked")
    private Map<ImmutableSpi, Object> loadTransients(Collection<ImmutableSpi> sources) {

        Set<Object> sourceIds = toSourceIds(sources);
        TransientResolver<Object, Object> resolver =
                ((TransientResolver<Object, Object>) this.resolver);
        Cache<Object, Object> cache = sqlClient.getCaches().getPropertyCache(prop);
        Ref<SortedMap<String, Object>> parameterMapRef = resolver.getParameterMapRef();
        SortedMap<String, Object> parameterMap = parameterMapRef != null ?
                standardParameterMap(parameterMapRef.getValue()) :
                null;
        Cache.Parameterized<Object, Object> parameterizedCache =
                cache instanceof Cache.Parameterized<?, ?> ?
                        (Cache.Parameterized<Object, Object>)cache :
                        null;
        final boolean useCache;
        if (cache != null) {
            if (parameterMapRef == null) {
                useCache = false;
                CacheAbandonedCallback callback = sqlClient.getCaches().getAbandonedCallback();
                if (callback != null) {
                    callback.abandoned(prop, CacheAbandonedCallback.Reason.CACHEABLE_FILTER_REQUIRED);
                }
            } else {
                if (parameterMap != null && !parameterMap.isEmpty() && parameterizedCache == null) {
                    useCache = false;
                    CacheAbandonedCallback callback = sqlClient.getCaches().getAbandonedCallback();
                    if (callback != null) {
                        callback.abandoned(prop, CacheAbandonedCallback.Reason.PARAMETERIZED_CACHE_REQUIRED);
                    }
                } else {
                    useCache = true;
                }
            }
        } else {
            useCache = false;
        }

        if (!useCache) {
            Map<Object, Object> fetchedMap;
            TransientResolverContext ctx = TransientResolverContext.push(con, resolver, sourceIds);
            try {
                fetchedMap = fetchResolvedMap(resolveWithDefaultValue(resolver, sourceIds));
            } finally {
                TransientResolverContext.pop(ctx);
            }
            return Utils.joinCollectionAndMap(
                    sources,
                    this::toSourceId,
                    fetchedMap
            );
        }

        CacheEnvironment<Object, Object> env = new CacheEnvironment<>(
                sqlClient,
                con,
                (ids) -> {
                    TransientResolverContext ctx = TransientResolverContext.push(con, resolver, ids);
                    try {
                        return resolveWithDefaultValue(resolver, ids);
                    } finally {
                        TransientResolverContext.pop(ctx);
                    }
                },
                false
        );
        Map<Object, Object> cachedMap =
                parameterMap != null && !parameterMap.isEmpty() && parameterizedCache != null ?
                        parameterizedCache.getAll(sourceIds, parameterMap, env) :
                        cache.getAll(sourceIds, env);
        return Utils.joinCollectionAndMap(
                sources,
                this::toSourceId,
                fetchResolvedMap(cachedMap)
        );
    }

    private Map<ImmutableSpi, ImmutableSpi> loadParents(Collection<ImmutableSpi> sources) {
        Cache<Object, Object> fkCache = sqlClient.getCaches().getPropertyCache(prop);
        SortedMap<String, Object> parameters = getParameters();
        if (!remote && !useCache(fkCache, parameters)) {
            return loadParentsDirectly(sources);
        }
        Map<Object, Object> fkMap = new LinkedHashMap<>(
                (sources.size() * 4 + 2) / 3
        );
        Collection<Object> missedFkSourceIds;
        missedFkSourceIds = new ArrayList<>();
        for (ImmutableSpi source : sources) {
            if (source.__isLoaded(prop.getId())) {
                ImmutableSpi target = (ImmutableSpi) source.__get(prop.getId());
                if (target != null) {
                    fkMap.put(toSourceId(source), toTargetId(target));
                }
            } else {
                missedFkSourceIds.add(toSourceId(source));
            }
        }
        if (!missedFkSourceIds.isEmpty()) {
            Map<Object, Object> missedFkMap;
            if (remote) {
                missedFkMap = queryForeignKeyMap(missedFkSourceIds);
            } else {
                CacheEnvironment<Object, Object> env = new CacheEnvironment<>(
                        sqlClient,
                        con,
                        this::queryForeignKeyMap,
                        false
                );
                missedFkMap = parameters != null ?
                        ((Cache.Parameterized<Object, Object>) fkCache).getAll(missedFkSourceIds, parameters, env) :
                        fkCache.getAll(missedFkSourceIds, env);
            }
            for (Object sourceId : missedFkSourceIds) {
                Object fk = missedFkMap.get(sourceId);
                if (fk != null) {
                    fkMap.put(sourceId, fk);
                }
            }
        }
        Map<Object, ImmutableSpi> targetMap =
                Utils.joinMaps(
                        fkMap,
                        Utils.toMap(
                                this::toTargetId,
                                findTargets(new LinkedHashSet<>(fkMap.values()))
                        )
                );
        return Utils.joinCollectionAndMap(sources, this::toSourceId, targetMap);
    }

    private Map<ImmutableSpi, ImmutableSpi> loadParentsDirectly(
            Collection<ImmutableSpi> sources
    ) {
        Map<Object, Object> fkMap = new LinkedHashMap<>(
                (sources.size() * 4 + 2) / 3
        );
        Collection<Object> missedFkSourceIds = new ArrayList<>();
        for (ImmutableSpi source : sources) {
            if (source.__isLoaded(prop.getId())) {
                ImmutableSpi target = (ImmutableSpi) source.__get(prop.getId());
                if (target != null) {
                    fkMap.put(toSourceId(source), toTargetId(target));
                }
            } else {
                missedFkSourceIds.add(toSourceId(source));
            }
        }
        Map<Object, ImmutableSpi> map1 = null;
        if (!fkMap.isEmpty()) {
            if (isUnreliableParentId()) {
                map1 = Utils.joinMaps(
                        fkMap,
                        Utils.toMap(
                                this::toTargetId,
                                queryTargets(fkMap.values())
                        )
                );
            } else {
                map1 = Utils.joinMaps(
                        fkMap,
                        Utils.toMap(
                                this::toTargetId,
                                findTargets(fkMap.values())
                        )
                );
            }
        }
        Map<Object, ImmutableSpi> map2 = null;
        if (!missedFkSourceIds.isEmpty()) {
            if (isUnreliableParentId() || fetcher.getFieldMap().size() > 1) {
                map2 = Tuple2.toMap(
                        querySourceTargetPairs(missedFkSourceIds)
                );
            } else {
                Map<Object, Object> loadedFkMap =
                        queryForeignKeyMap(missedFkSourceIds);
                map2 = new LinkedHashMap<>((missedFkSourceIds.size() * 4 + 2) / 3);
                for (Object sourceId : missedFkSourceIds) {
                    Object targetId = loadedFkMap.get(sourceId);
                    map2.put(sourceId, makeIdOnlyTarget(targetId));
                }
            }
        }
        return Utils.joinCollectionAndMap(
                sources,
                this::toSourceId,
                Utils.mergeMap(map1, map2)
        );
    }

    private Map<ImmutableSpi, ImmutableSpi> loadTargetMap(Collection<ImmutableSpi> sources) {
        Cache<Object, Object> cache = sqlClient.getCaches().getPropertyCache(prop);
        SortedMap<String, Object> parameters = getParameters();
        if (!remote && !useCache(cache, parameters)) {
            return loadTargetMapDirectly(sources);
        }
        if (remote && prop.getMappedBy() != null) {
            List<Tuple2<Object, ImmutableSpi>> tuples;
            try {
                tuples = sqlClient
                        .getMicroServiceExchange()
                        .findByAssociatedIds(
                                prop.getTargetType().getMicroServiceName(),
                                prop.getMappedBy(),
                                toSourceIds(sources),
                                FetcherFactory.excludeMicroServiceNameExceptRoot(fetcher, prop.getDeclaringType().getMicroServiceName())
                        );
            } catch (Exception ex) {
                throw new ExecutionException(
                        "Cannot load the remote association \"" +
                                prop +
                                "\" because error raised",
                        ex
                );
            }
            return Utils.joinCollectionAndMap(
                    sources,
                    this::toSourceId,
                    Tuple2.toMap(tuples)
            );
        }
        Set<Object> sourceIds = toSourceIds(sources);
        Map<Object, Object> idMap;
        if (remote) {
            idMap = Tuple2.toMap(querySourceTargetIdPairs(sourceIds));
        } else {
            CacheEnvironment<Object, Object> env = new CacheEnvironment<>(
                    sqlClient,
                    con,
                    it -> Tuple2.toMap(
                            querySourceTargetIdPairs(it)
                    ),
                    false
            );
            idMap = parameters != null ?
                    ((Cache.Parameterized<Object, Object>) cache).getAll(sourceIds, parameters, env) :
                    cache.getAll(sourceIds, env);
        }
        Map<Object, ImmutableSpi> targetMap = Utils.toMap(
                this::toTargetId,
                findTargets(new LinkedHashSet<>(idMap.values()))
        );
        return Utils.joinCollectionAndMap(
                sources,
                this::toSourceId,
                Utils.joinMaps(idMap, targetMap)
        );
    }

    private Map<ImmutableSpi, ImmutableSpi> loadTargetMapDirectly(Collection<ImmutableSpi> sources) {
        Set<Object> sourceIds = toSourceIds(sources);
        Map<Object, ImmutableSpi> targetMap;
        if (globalFiler != null || propFilter != null || fetcher.getFieldMap().size() > 1) {
            targetMap = Tuple2.toMap(
                    querySourceTargetPairs(sourceIds)
            );
        } else {
            targetMap = Tuple2.toMap(
                    querySourceTargetIdPairs(sourceIds),
                    this::makeIdOnlyTarget
            );
        }
        return Utils.joinCollectionAndMap(
                sources,
                this::toSourceId,
                targetMap
        );
    }

    private Map<ImmutableSpi, List<ImmutableSpi>> loadTargetMultiMap(Collection<ImmutableSpi> sources) {
        Cache<Object, List<Object>> cache = sqlClient.getCaches().getPropertyCache(prop);
        SortedMap<String, Object> parameters = getParameters();
        if (!remote && !useCache(cache, parameters)) {
            return loadTargetMultiMapDirectly(sources);
        }
        if (remote && prop.getMappedBy() != null) {
            List<Tuple2<Object, ImmutableSpi>> tuples;
            try {
                tuples = sqlClient
                        .getMicroServiceExchange()
                        .findByAssociatedIds(
                                prop.getTargetType().getMicroServiceName(),
                                prop.getMappedBy(),
                                toSourceIds(sources),
                                FetcherFactory.excludeMicroServiceNameExceptRoot(fetcher, prop.getDeclaringType().getMicroServiceName())
                        );
            } catch (Exception ex) {
                throw new ExecutionException(
                        "Cannot load the remote association \"" +
                                prop +
                                "\" because error raised",
                        ex
                );
            }
            return Utils.joinCollectionAndMap(
                    sources,
                    this::toSourceId,
                    Tuple2.toMultiMap(tuples)
            );
        }
        Set<Object> sourceIds = toSourceIds(sources);
        Map<Object, List<Object>> idMultiMap;
        if (remote) {
            idMultiMap = Tuple2.toMultiMap(
                    querySourceTargetIdPairs(sourceIds)
            );
        } else {
            CacheEnvironment<Object, List<Object>> env = new CacheEnvironment<>(
                    sqlClient,
                    con,
                    it -> Tuple2.toMultiMap(
                            querySourceTargetIdPairs(it)
                    ),
                    false
            );
            idMultiMap = parameters != null ?
                    ((Cache.Parameterized<Object, List<Object>>) cache).getAll(sourceIds, parameters, env) :
                    cache.getAll(sourceIds, env);
        }
        Map<Object, ImmutableSpi> targetMap = Utils.toMap(
                this::toTargetId,
                findTargets(
                        idMultiMap
                                .values()
                                .stream()
                                .filter(Objects::nonNull)
                                .flatMap(Collection::stream)
                                .distinct()
                                .collect(Collectors.toList())
                )
        );
        return Utils.joinCollectionAndMap(
                sources,
                this::toSourceId,
                Utils.joinMultiMapAndMap(idMultiMap, targetMap)
        );
    }

    private Map<ImmutableSpi, List<ImmutableSpi>> loadTargetMultiMapDirectly(Collection<ImmutableSpi> sources) {
        Set<Object> sourceIds = toSourceIds(sources);
        Map<Object, List<ImmutableSpi>> targetMap;
        if (globalFiler != null || propFilter != null || fetcher.getFieldMap().size() > 1) {
            targetMap = Tuple2.toMultiMap(
                    querySourceTargetPairs(sourceIds)
            );
        } else {
            targetMap = Tuple2.toMultiMap(
                    querySourceTargetIdPairs(sourceIds),
                    this::makeIdOnlyTarget
            );
        }
        return Utils.joinCollectionAndMap(
                sources,
                this::toSourceId,
                targetMap
        );
    }

    private Map<Object, Object> queryForeignKeyMap(Collection<Object> sourceIds) {

        if (sourceIds.size() == 1) {
            Object sourceId = CollectionUtils.first(sourceIds);
            List<Object> targetIds = Queries.createQuery(sqlClient, prop.getDeclaringType(), ExecutionPurpose.LOAD, FilterLevel.IGNORE_ALL, (q, source) -> {
                Expression<Object> pkExpr = source.get(sourceIdProp);
                Table<?> targetTable = source.join(prop);
                Expression<Object> fkExpr = source.getAssociatedId(prop);
                q.where(pkExpr.eq(sourceId));
                q.where(fkExpr.isNotNull());
                applyPropFilter(q, targetTable, sourceIds);
                applyGlobalFilter(q, targetTable);
                applyDefaultOrder(q, targetTable);
                return q.select(fkExpr);
            }).execute(con);
            return Utils.toMap(sourceId, targetIds);
        }
        List<Tuple2<Object, Object>> tuples = Queries
                .createQuery(sqlClient, prop.getDeclaringType(), ExecutionPurpose.LOAD, FilterLevel.IGNORE_ALL, (q, source) -> {
                    Expression<Object> pkExpr = source.get(sourceIdProp);
                    Table<?> targetTable = source.join(prop);
                    Expression<Object> fkExpr = source.getAssociatedId(prop);
                    q.where(pkExpr.in(sourceIds));
                    q.where(fkExpr.isNotNull());
                    applyPropFilter(q, targetTable, sourceIds);
                    applyGlobalFilter(q, targetTable);
                    applyDefaultOrder(q, targetTable);
                    return q.select(pkExpr, fkExpr);
                }).execute(con);
        return Tuple2.toMap(tuples);
    }

    private List<Tuple2<Object, Object>> querySourceTargetIdPairs(Collection<Object> sourceIds) {
        if (propFilter == null && prop.getReal().isMiddleTableDefinition()) {
            if (sourceIds.size() == 1) {
                Object sourceId = CollectionUtils.first(sourceIds);
                List<Object> targetIds = Queries.createAssociationQuery(sqlClient, AssociationType.of(prop), ExecutionPurpose.LOAD, (q, association) -> {
                    Expression<Object> sourceIdExpr = association.sourceId();
                    Expression<Object> targetIdExpr = association.targetId();
                    q.where(sourceIdExpr.eq(sourceId));
                    applyPropFilter(q, association.target(), sourceIds);
                    applyGlobalFilter(q, association.target());
                    applyDefaultOrder(q, association.target());
                    return q.select(targetIdExpr);
                }).limit(limit, offset).execute(con);
                return Utils.toTuples(sourceId, targetIds);
            }
            if (limit != Integer.MAX_VALUE || offset != 0) {
                TypedRootQuery<Tuple2<Object, Object>>[] queries = new TypedRootQuery[sourceIds.size()];
                int index = 0;
                for (Object sourceId : sourceIds) {
                    queries[index++] =
                            Queries.createAssociationQuery(sqlClient, AssociationType.of(prop), ExecutionPurpose.LOAD, (q, association) -> {
                                Expression<Object> sourceIdExpr = association.sourceId();
                                Expression<Object> targetIdExpr = association.targetId();
                                q.where(sourceIdExpr.eq(sourceId));
                                applyPropFilter(q, association.target(), Collections.singletonList(sourceId));
                                applyGlobalFilter(q, association.target());
                                applyDefaultOrder(q, association.target());
                                return q.select(sourceIdExpr, targetIdExpr);
                            }).limit(limit, offset);
                }
                return MergedTypedRootQueryImpl.of("union all", queries).execute(con);
            }
            return Queries.createAssociationQuery(sqlClient, AssociationType.of(prop), ExecutionPurpose.LOAD, (q, association) -> {
                Expression<Object> sourceIdExpr = association.sourceId();
                Expression<Object> targetIdExpr = association.targetId();
                q.where(sourceIdExpr.in(sourceIds));
                applyPropFilter(q, association.target(), sourceIds);
                applyGlobalFilter(q, association.target());
                applyDefaultOrder(q, association.target());
                return q.select(sourceIdExpr, targetIdExpr);
            }).execute(con);
        }
        return executeTupleQuery(sourceIds, target -> target.get(targetIdProp.getName()));
    }

    @SuppressWarnings("unchecked")
    private List<Tuple2<Object, ImmutableSpi>> querySourceTargetPairs(
            Collection<Object> sourceIds
    ) {
        return executeTupleQuery(sourceIds, target -> new FetcherSelectionImpl<>(target, path, fetcher));
    }

    @SuppressWarnings("unchecked")
    private List<ImmutableSpi> queryTargets(Collection<Object> targetIds) {
        return Queries.createQuery(sqlClient, prop.getTargetType(), ExecutionPurpose.LOAD, FilterLevel.IGNORE_ALL, (q, target) -> {
            Expression<Object> idExpr = target.get(targetIdProp.getName());
            q.where(idExpr.in(targetIds));
            applyPropFilter(q, target, targetIds);
            applyGlobalFilter(q, target);
            applyDefaultOrder(q, target);
            return q.select(
                    new FetcherSelectionImpl<>((Table<ImmutableSpi>)target, path, fetcher)
            );
        }).execute(con);
    }

    @SuppressWarnings("unchecked")
    private <R> List<Tuple2<Object, R>> executeTupleQuery(
            Collection<Object> sourceIds,
            Function<Table<ImmutableSpi>, Selection<?>> valueExpressionGetter
    ) {
        if (sourceIds.size() == 1) {
            Object sourceId = CollectionUtils.first(sourceIds);
            List<R> results = Queries.createQuery(sqlClient, prop.getTargetType(), ExecutionPurpose.LOAD, FilterLevel.IGNORE_ALL, (q, target) -> {
                Expression<Object> sourceIdExpr = target.inverseGetAssociatedId(prop);
                q.where(sourceIdExpr.eq(sourceId));
                applyPropFilter(q, target, sourceIds);
                applyGlobalFilter(q, target);
                applyDefaultOrder(q, target);
                return q.select((Selection<R>) valueExpressionGetter.apply((Table<ImmutableSpi>) target));
            }).limit(limit, offset).execute(con);
            return Utils.toTuples(sourceId, results);
        }
        if (limit != Integer.MAX_VALUE || offset != 0) {
            TypedRootQuery<Tuple2<Object, R>>[] queries = new TypedRootQuery[sourceIds.size()];
            int index = 0;
            for (Object sourceId : sourceIds) {
                queries[index++] =
                        Queries.createQuery(sqlClient, prop.getTargetType(), ExecutionPurpose.LOAD, FilterLevel.IGNORE_ALL, (q, target) -> {
                            Expression<Object> sourceIdExpr = target.inverseGetAssociatedId(prop);
                            q.where(sourceIdExpr.eq(sourceId));
                            applyPropFilter(q, target, Collections.singletonList(sourceId));
                            applyGlobalFilter(q, target);
                            applyDefaultOrder(q, target);
                            return q.select(sourceIdExpr, (Selection<R>) valueExpressionGetter.apply((Table<ImmutableSpi>) target));
                        }).limit(limit, offset);
            }
            return MergedTypedRootQueryImpl.of("union all", queries).execute(con);
        }
        return Queries.createQuery(sqlClient, prop.getTargetType(), ExecutionPurpose.LOAD, FilterLevel.IGNORE_ALL, (q, target) -> {
            Expression<Object> sourceIdExpr = target.inverseGetAssociatedId(prop);
            q.where(sourceIdExpr.in(sourceIds));
            applyPropFilter(q, target, sourceIds);
            applyGlobalFilter(q, target);
            applyDefaultOrder(q, target);
            return q.select(sourceIdExpr, (Selection<R>) valueExpressionGetter.apply((Table<ImmutableSpi>) target));
        }).execute(con);
    }

    private void applyGlobalFilter(Sortable sortable, Table<?> table) {
        AbstractMutableQueryImpl query = (AbstractMutableQueryImpl) sortable;
        query.setOrderByPriority(AbstractMutableQueryImpl.ORDER_BY_PRIORITY_GLOBAL_FILTER);
        TableImplementor<?> tableImplementor = null;
        if (table instanceof TableImplementor<?>) {
            tableImplementor = (TableImplementor<?>) table;
        } else if (table instanceof TableProxy<?>) {
            tableImplementor = ((TableProxy<?>) table).__unwrap();
        }
        if (tableImplementor == null) {
            throw new AssertionError(
                    "The table create by data loader must be table implementation or table wrapper"
            );
        }
        query.applyDataLoaderGlobalFilters(tableImplementor);
    }

    @SuppressWarnings("unchecked")
    private void applyPropFilter(
            MutableQuery query,
            Table<?> table,
            Collection<Object> keys
    ) {
        if (propFilter != null) {
            FieldFilterArgsImpl<Table<ImmutableSpi>> args = FieldFilterArgsImpl.of(
                    (AbstractMutableQueryImpl) query,
                    (Table<ImmutableSpi>) table,
                    keys
            );
            ((AbstractMutableQueryImpl)query)
                    .setOrderByPriority(AbstractMutableQueryImpl.ORDER_BY_PRIORITY_PROP_FILTER);
            propFilter.apply(args);
        }
    }

    private void applyDefaultOrder(
            MutableQuery query,
            Table<?> table
    ) {
        if (((AbstractMutableQueryImpl)query).getAcceptedOrderByPriority() >
                AbstractMutableQueryImpl.ORDER_BY_PRIORITY_STATEMENT) {
            return;
        }
        List<OrderedItem> orderedItems = prop.getOrderedItems();
        if (!orderedItems.isEmpty() && !remote) {
            for (OrderedItem orderedItem : orderedItems) {
                Expression<?> expr = table.get(orderedItem.getProp().getName());
                if (orderedItem.isDesc()) {
                    query.orderBy(expr.desc());
                } else {
                    query.orderBy(expr);
                }
            }
        }
    }

    private Object toSourceId(ImmutableSpi source) {
        return source.__get(sourceIdProp.getId());
    }

    private Set<Object> toSourceIds(Collection<ImmutableSpi> sources) {
        Set<Object> sourceIds = new LinkedHashSet<>((sources.size() * 4 + 2) / 3);
        for (ImmutableSpi source : sources) {
            sourceIds.add(toSourceId(source));
        }
        return sourceIds;
    }

    private Object toTargetId(ImmutableSpi target) {
        if (target == null) {
            return null;
        }
        return target.__get(targetIdProp.getId());
    }

    @SuppressWarnings("unchecked")
    private List<ImmutableSpi> findTargets(Collection<Object> targetIds) {
        if (fetcher.getFieldMap().size() < 2 && !remote) {
            return makeIdOnlyTargets(targetIds);
        }
        if (remote) {
            try {
                return sqlClient.getMicroServiceExchange().findByIds(
                        prop.getTargetType().getMicroServiceName(),
                        targetIds,
                        FetcherFactory.excludeMicroServiceNameExceptRoot(fetcher, prop.getDeclaringType().getMicroServiceName())
                );
            } catch (Exception ex) {
                throw new ExecutionException(
                        "Cannot load the remote association \"" +
                                prop +
                                "\" because error raised",
                        ex
                );
            }
        }
        return ((EntitiesImpl)sqlClient.getEntities()).forLoader().forConnection(con).findByIds(
                fetcher,
                targetIds
        );
    }

    private List<ImmutableSpi> makeIdOnlyTargets(Collection<Object> targetIds) {
        return targetIds
                .stream()
                .map(this::makeIdOnlyTarget)
                .collect(
                        Collectors.toCollection(
                                () -> new ArrayList<>(targetIds.size())
                        )
                );
    }

    private ImmutableSpi makeIdOnlyTarget(Object id) {
        if (id == null) {
            return null;
        }
        return (ImmutableSpi) Internal.produce(prop.getTargetType(), null, draft -> {
            DraftSpi targetDraft = (DraftSpi) draft;
            targetDraft.__set(targetIdProp.getId(), id);
        });
    }

    private SortedMap<String, Object> getParameters() {
        Filter<?> filter = globalFiler;
        if (filter instanceof CacheableFilter<?>) {
            SortedMap<String, Object> parameters = standardParameterMap(((CacheableFilter<?>) filter).getParameters());
            if (parameters.isEmpty()) {
                return null;
            }
            return parameters;
        }
        if (filter != null) {
            return ILLEGAL_PARAMETERS;
        }
        return null;
    }

    private boolean useCache(Cache<?, ?> cache, Map<String, Object> parameters) {
        if (cache == null) {
            return false;
        }
        if (remote) {
            return true;
        }
        if (propFilter != null) {
            CacheAbandonedCallback callback = sqlClient.getCaches().getAbandonedCallback();
            if (callback != null) {
                callback.abandoned(prop, CacheAbandonedCallback.Reason.FIELD_FILTER_USED);
            }
            return false;
        }
        if (parameters == ILLEGAL_PARAMETERS) {
            CacheAbandonedCallback callback = sqlClient.getCaches().getAbandonedCallback();
            if (callback != null) {
                callback.abandoned(prop, CacheAbandonedCallback.Reason.CACHEABLE_FILTER_REQUIRED);
            }
            return false;
        }
        if (parameters != null && !(cache instanceof Cache.Parameterized<?, ?>)) {
            CacheAbandonedCallback callback = sqlClient.getCaches().getAbandonedCallback();
            if (callback != null) {
                callback.abandoned(prop, CacheAbandonedCallback.Reason.PARAMETERIZED_CACHE_REQUIRED);
            }
            return false;
        }
        return true;
    }

    private Map<Object, Object> resolveWithDefaultValue(
            TransientResolver<Object, Object> resolver,
            Collection<Object> ids
    ) {
        Map<Object, Object> valueMap = resolver.resolve(ids);
        if (valueMap.keySet().containsAll(ids)) {
            return valueMap;
        }
        Object defaultValue = resolver.getDefaultValue();
        if (defaultValue == null && (prop.isReferenceList(TargetLevel.OBJECT))) {
            defaultValue = Collections.emptyList();
        }
        for (Object id : ids) {
            if (!valueMap.containsKey(id)) {
                valueMap.put(id, defaultValue);
            }
        }
        return valueMap;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> fetchResolvedMap(Map<Object, Object> map) {
        if (map.isEmpty() || !prop.isAssociation(TargetLevel.OBJECT) || !prop.getTargetType().isEntity()) {
            return map;
        }
        Collection<Object> targetIds = new LinkedHashSet<>();
        if (prop.isReferenceList(TargetLevel.OBJECT)) {
            for (Object mapValue : map.values()) {
                if (mapValue != null) {
                    for (Object targetId : (Collection<Object>) mapValue) {
                        if (targetId != null) {
                            targetIds.add(targetId);
                        }
                    }
                }
            }
        } else {
            for (Object targetId : map.values()) {
                if (targetId != null) {
                    targetIds.add(targetId);
                }
            }
        }

        boolean noFilter = propFilter == null && globalFiler == null;
        List<ImmutableSpi> targets;
        if (noFilter) {
            targets = findTargets(targetIds);
        } else {
            targets = Queries.createQuery(sqlClient, prop.getTargetType(), ExecutionPurpose.LOAD, FilterLevel.IGNORE_ALL, (q, target) -> {
                Expression<Object> pkExpr = target.get(targetIdProp.getName());
                q.where(pkExpr.in(targetIds));
                applyPropFilter(q, target, map.keySet());
                applyGlobalFilter(q, target);
                return q.select(
                        new FetcherSelectionImpl<>((Table<ImmutableSpi>)target, path, fetcher)
                );
            }).execute(con);
        }

        if (targets.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<Object, ImmutableSpi> targetMap = new HashMap<>((targets.size() * 4 + 2) / 3);
        PropId targetIdPropId = prop.getTargetType().getIdProp().getId();
        for (ImmutableSpi target : targets) {
            targetMap.put(target.__get(targetIdPropId), target);
        }

        Map<Object, Object> fetchedMap = new LinkedHashMap<>((map.size() * 4 + 2) / 3);
        if (noFilter && prop.isReferenceList(TargetLevel.ENTITY)) {
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                Collection<Object> subCollection = (Collection<Object>) e.getValue();
                if (subCollection != null) {
                    List<ImmutableSpi> targetList = new ArrayList<>(subCollection.size());
                    for (Object targetId : subCollection) {
                        ImmutableSpi target = targetMap.get(targetId);
                        if (target != null) {
                            targetList.add(target);
                        }
                    }
                    fetchedMap.put(e.getKey(), targetList);
                } else {
                    fetchedMap.put(e.getKey(), Collections.emptyList());
                }
            }
        } else if (!noFilter && prop.isReferenceList(TargetLevel.ENTITY)) {
            IdentityHashMap<ImmutableSpi, Object> identityMap = new IdentityHashMap<>();
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                Collection<Object> subCollection = (Collection<Object>) e.getValue();
                if (subCollection != null) {
                    for (Object targetId : subCollection) {
                        ImmutableSpi target = targetMap.get(targetId);
                        if (target != null) {
                            identityMap.put(target, e.getKey());
                        }
                    }
                }
            }
            for (ImmutableSpi target : targets) {
                Object key = identityMap.get(target);
                if (key != null) {
                    List<Object> targetList = (List<Object>) fetchedMap.get(key);
                    if (targetList == null) {
                        Collection<?> ids = (Collection<?>) map.get(key);
                        if (ids != null) {
                            targetList = new ArrayList<>(ids.size());
                        } else {
                            targetList = new ArrayList<>();
                        }
                        fetchedMap.put(key, targetList);
                    }
                    targetList.add(target);
                }
            }
        } else {
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                ImmutableSpi target = targetMap.get(e.getValue());
                if (target != null) {
                    fetchedMap.put(e.getKey(), target);
                }
            }
        }
        return fetchedMap;
    }

    private boolean isUnreliableParentId() {
        if (remote) {
            return false;
        }
        if (rawValue) {
            return false;
        }
        return !((ColumnDefinition) storage).isForeignKey() ||
                globalFiler != null ||
                propFilter != null;
    }

    private static SortedMap<String, Object> standardParameterMap(SortedMap<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return parameters;
        }
        boolean hasNullValue = false;
        for (Object o : parameters.values()) {
            if (o == null) {
                hasNullValue = true;
                break;
            }
        }
        if (!hasNullValue) {
            return parameters;
        }
        SortedMap<String, Object> withoutNullValueMap = new TreeMap<>();
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            Object value = e.getValue();
            if (value != null) {
                withoutNullValueMap.put(e.getKey(), value);
            }
        }
        return withoutNullValueMap;
    }
}