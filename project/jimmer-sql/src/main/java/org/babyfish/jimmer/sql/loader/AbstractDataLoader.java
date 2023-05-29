package org.babyfish.jimmer.sql.loader;

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
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.impl.query.SortableImplementor;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheAbandonedCallback;
import org.babyfish.jimmer.sql.cache.CacheEnvironment;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherFactory;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.fetcher.impl.FieldFilterArgsImpl;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.impl.FilterArgsImpl;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractDataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDataLoader.class);

    private static final ThreadLocal<Connection> TRANSIENT_RESOLVER_CON_LOCAL = new ThreadLocal<>();

    /* For globalFilter is not null but not `Filter.Parameterized` */
    private static final SortedMap<String, Object> ILLEGAL_PARAMETERS =
            Collections.unmodifiableSortedMap(new TreeMap<>());

    private final JSqlClientImplementor sqlClient;

    private final Connection con;

    private final ImmutableProp prop;

    private final Storage storage;

    private final boolean remote;
    
    private final ImmutableProp sourceIdProp;
    
    private final ImmutableProp targetIdProp;

    private final org.babyfish.jimmer.sql.filter.Filter<Props> globalFiler;

    private final FieldFilter<Table<ImmutableSpi>> propFilter;

    private final int limit;

    private final int offset;

    private final TransientResolver<?, ?> resolver;

    private final Fetcher<ImmutableSpi> fetcher;

    @SuppressWarnings("unchecked")
    protected AbstractDataLoader(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType entityType,
            ImmutableProp prop,
            Fetcher<?> fetcher,
            FieldFilter<?> propFilter,
            int limit,
            int offset
    ) {
        if (!prop.isAssociation(TargetLevel.ENTITY) && !prop.hasTransientResolver()) {
            throw new IllegalArgumentException(
                    "\"" + prop + "\" is neither association nor transient with resolver"
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
        if (prop.isReference(TargetLevel.ENTITY) && !prop.isNullable()) {
            if (globalFiler != null) {
                throw new ExecutionException(
                        "Cannot apply filter \"" +
                                globalFiler +
                                "\" for \"" +
                                prop +
                                "\" because that property is not nullable"
                );
            }
            if (propFilter != null) {
                throw new ExecutionException(
                        "Cannot apply field filter of object fetcher for \"" +
                                prop +
                                "\" because that property is not nullable"
                );
            }
        }
        this.limit = limit;
        this.offset = offset;
        if (prop.isAssociation(TargetLevel.PERSISTENT)) {
            this.resolver = null;
            this.fetcher = fetcher != null ?
                    (Fetcher<ImmutableSpi>) fetcher :
                    new FetcherImpl<>((Class<ImmutableSpi>) prop.getTargetType().getJavaClass());
        } else {
            this.resolver = sqlClient.getResolver(prop);
            if (prop.isAssociation(TargetLevel.ENTITY)) {
                this.fetcher = fetcher != null ?
                        (Fetcher<ImmutableSpi>) fetcher :
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
        if (sources.size() > 1 && (limit != Integer.MAX_VALUE || offset != 0)) {
            throw new IllegalArgumentException("Pagination data loader does not support batch loading");
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

        Collection<Object> sourceIds = toSourceIds(sources);
        TransientResolver<Object, Object> resolver =
                ((TransientResolver<Object, Object>) this.resolver);
        Cache<Object, Object> cache = sqlClient.getCaches().getPropertyCache(prop);
        Ref<SortedMap<String, Object>> parameterMapRef = resolver.getParameterMapRef();
        SortedMap<String, Object> parameterMap = parameterMapRef != null ?
                parameterMapRef.getValue() :
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
            Map<Object, Object> resolvedMap;
            TRANSIENT_RESOLVER_CON_LOCAL.set(con);
            try {
                resolvedMap = translateResolvedMap(resolver.resolve(sourceIds), sourceIds);
            } finally {
                TRANSIENT_RESOLVER_CON_LOCAL.remove();
            }
            return Utils.joinCollectionAndMap(
                    sources,
                    this::toSourceId,
                    resolvedMap
            );
        }

        CacheEnvironment<Object, Object> env = new CacheEnvironment<>(
                sqlClient,
                con,
                (ids) -> {
                    TRANSIENT_RESOLVER_CON_LOCAL.set(con);
                    try {
                        return resolver.resolve(ids);
                    } finally {
                        TRANSIENT_RESOLVER_CON_LOCAL.remove();
                    }
                },
                false
        );
        Map<Object, Object> valueMap =
                parameterMap != null && !parameterMap.isEmpty() && parameterizedCache != null ?
                        parameterizedCache.getAll(sourceIds, parameterMapRef.getValue(), env) :
                        cache.getAll(sourceIds, env);
        return Utils.joinCollectionAndMap(
                sources,
                this::toSourceId,
                translateResolvedMap(valueMap, sourceIds)
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
        if (isUnreliableParentId()) {
            missedFkSourceIds = toSourceIds(sources);
        } else {
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
        List<Object> sourceIds = toSourceIds(sources);
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
        List<Object> sourceIds = toSourceIds(sources);
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
        List<Object> sourceIds = toSourceIds(sources);
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
        List<Object> sourceIds = toSourceIds(sources);
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
            Object sourceId = sourceIds.iterator().next();
            List<Object> targetIds = Queries.createQuery(sqlClient, prop.getDeclaringType(), ExecutionPurpose.LOAD, true, (q, source) -> {
                Expression<Object> pkExpr = source.get(sourceIdProp.getName());
                Table<?> targetTable = source.join(prop.getName());
                Expression<Object> fkExpr = targetTable.get(targetIdProp.getName());
                q.where(pkExpr.eq(sourceId));
                q.where(fkExpr.isNotNull());
                if (!applyPropFilter(q, targetTable, sourceIds) & !applyGlobalFilter(q, targetTable)) {
                    applyDefaultOrder(q, targetTable);
                }
                return q.select(fkExpr);
            }).limit(limit, offset).execute(con);
            return Utils.toMap(sourceId, targetIds);
        }
        List<Tuple2<Object, Object>> tuples = Queries
                .createQuery(sqlClient, prop.getDeclaringType(), ExecutionPurpose.LOAD, true, (q, source) -> {
                    Expression<Object> pkExpr = source.get(sourceIdProp.getName());
                    Table<?> targetTable = source.join(prop.getName());
                    Expression<Object> fkExpr = targetTable.get(targetIdProp.getName());
                    q.where(pkExpr.in(sourceIds));
                    q.where(fkExpr.isNotNull());
                    if (!applyPropFilter(q, targetTable, sourceIds) & !applyGlobalFilter(q, targetTable)) {
                        applyDefaultOrder(q, targetTable);
                    }
                    return q.select(pkExpr, fkExpr);
                }).execute(con);
        return Tuple2.toMap(tuples);
    }

    private List<Tuple2<Object, Object>> querySourceTargetIdPairs(Collection<Object> sourceIds) {
        if (propFilter == null && prop.getReal().isMiddleTableDefinition()) {
            if (sourceIds.size() == 1) {
                Object sourceId = sourceIds.iterator().next();
                List<Object> targetIds = Queries.createAssociationQuery(sqlClient, AssociationType.of(prop), ExecutionPurpose.LOAD, (q, association) -> {
                    Expression<Object> sourceIdExpr = association.source(prop.getDeclaringType()).get(sourceIdProp.getName());
                    Expression<Object> targetIdExpr = association.target().get(targetIdProp.getName());
                    q.where(sourceIdExpr.eq(sourceId));
                    applyPropFilter(q, association.target(), sourceIds);
                    applyGlobalFilter(q, association.target());
                    return q.select(targetIdExpr);
                }).limit(limit, offset).execute(con);
                return Utils.toTuples(sourceId, targetIds);
            }
            return Queries.createAssociationQuery(sqlClient, AssociationType.of(prop), ExecutionPurpose.LOAD, (q, association) -> {
                Expression<Object> sourceIdExpr = association.source(prop.getDeclaringType()).get(sourceIdProp.getName());
                Expression<Object> targetIdExpr = association.target().get(targetIdProp.getName());
                q.where(sourceIdExpr.in(sourceIds));
                applyPropFilter(q, association.target(), sourceIds);
                applyGlobalFilter(q, association.target());
                return q.select(sourceIdExpr, targetIdExpr);
            }).execute(con);
        }
        return executeTupleQuery(sourceIds, target -> target.get(targetIdProp.getName()));
    }

    @SuppressWarnings("unchecked")
    private List<Tuple2<Object, ImmutableSpi>> querySourceTargetPairs(
            Collection<Object> sourceIds
    ) {
        return executeTupleQuery(sourceIds, target -> target.fetch(fetcher));
    }

    @SuppressWarnings("unchecked")
    private List<ImmutableSpi> queryTargets(Collection<Object> targetIds) {

        return Queries.createQuery(sqlClient, prop.getTargetType(), ExecutionPurpose.LOAD, true, (q, target) -> {
            Expression<Object> idExpr = target.get(targetIdProp.getName());
            q.where(idExpr.in(targetIds));
            if (!applyPropFilter(q, target, targetIds) & !applyGlobalFilter(q, target)) {
                applyDefaultOrder(q, target);
            }
            return q.select(
                    ((Table<ImmutableSpi>)target).fetch(fetcher)
            );
        }).execute(con);
    }

    @SuppressWarnings("unchecked")
    private <R> List<Tuple2<Object, R>> executeTupleQuery(
            Collection<Object> sourceIds,
            Function<Table<ImmutableSpi>, Selection<?>> valueExpressionGetter
    ) {
        if (sourceIds.size() == 1) {
            Object sourceId = sourceIds.iterator().next();
            List<R> results = Queries.createQuery(sqlClient, prop.getTargetType(), ExecutionPurpose.LOAD, true, (q, target) -> {
                Expression<Object> sourceIdExpr = target
                        .inverseJoin(prop)
                        .get(sourceIdProp.getName());
                q.where(sourceIdExpr.eq(sourceId));
                if (!applyPropFilter(q, target, sourceIds) & !applyGlobalFilter(q, target) ) {
                    applyDefaultOrder(q, target);
                }
                return q.select((Selection<R>) valueExpressionGetter.apply((Table<ImmutableSpi>) target));
            }).limit(limit, offset).execute(con);
            return Utils.toTuples(sourceId, results);
        }
        return Queries.createQuery(sqlClient, prop.getTargetType(), ExecutionPurpose.LOAD, true, (q, target) -> {
            Expression<Object> sourceIdExpr = target
                    .inverseJoin(prop)
                    .get(sourceIdProp.getName());
            q.where(sourceIdExpr.in(sourceIds));
            if (!applyPropFilter(q, target, sourceIds) & !applyGlobalFilter(q, target)) {
                applyDefaultOrder(q, target);
            }
            return q.select(sourceIdExpr, (Selection<R>) valueExpressionGetter.apply((Table<ImmutableSpi>) target));
        }).execute(con);
    }

    private boolean applyGlobalFilter(Sortable sortable, Table<?> table) {
        if (remote) {
            return false;
        }
        SortableImplementor sortableImplementor = (SortableImplementor)sortable;
        Filter<Props> globalFiler = this.globalFiler;
        if (globalFiler instanceof CacheableFilter<?>) {
            sortableImplementor.disableSubQuery();
            try {
                FilterArgsImpl<Props> args = new FilterArgsImpl<>(sortableImplementor, table, true);
                globalFiler.filter(args);
                return args.isSorted();
            } finally {
                sortableImplementor.enableSubQuery();
            }
        } else if (globalFiler != null) {
            FilterArgsImpl<Props> args = new FilterArgsImpl<>(sortableImplementor, table, false);
            globalFiler.filter(args);
            return args.isSorted();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean applyPropFilter(
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
            propFilter.apply(args);
            return args.isSorted();
        }
        return false;
    }

    private void applyDefaultOrder(
            MutableQuery query,
            Table<?> table
    ) {
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

    private List<Object> toSourceIds(Collection<ImmutableSpi> sources) {
        return sources
                .stream()
                .map(this::toSourceId)
                .collect(Collectors.toList());
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
            SortedMap<String, Object> parameters = ((CacheableFilter<?>) filter).getParameters();
            if (parameters != null && parameters.isEmpty()) {
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

    private Map<Object, Object> translateResolvedMap(Map<Object, Object> map, Collection<Object> keys) {
        map = fetchResolvedMap(map);
        Object defaultValue = resolver.getDefaultValue();
        if (defaultValue == null && prop.isReferenceList(TargetLevel.OBJECT)) {
            defaultValue = Collections.emptyList();
        }
        if (defaultValue != null) {
            for (Object key : keys) {
                if (map.get(key) == null) {
                    map.putIfAbsent(key, defaultValue);
                }
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> fetchResolvedMap(Map<Object, Object> map) {
        if (map.isEmpty() || !prop.isAssociation(TargetLevel.OBJECT) || !prop.getTargetType().isEntity()) {
            return map;
        }
        Collection<Object> targetIds = new LinkedHashSet<>();
        if (prop.isReferenceList(TargetLevel.OBJECT)) {
            for (Object mapValue : map.values()) {
                for (Object targetId : (Collection<Object>)mapValue) {
                    if (targetId != null) {
                        targetIds.add(targetId);
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
            targets = Queries.createQuery(sqlClient, prop.getTargetType(), ExecutionPurpose.LOAD, true, (q, target) -> {
                Expression<Object> pkExpr = target.get(targetIdProp.getName());
                q.where(pkExpr.in(targetIds));
                applyPropFilter(q, target, map.keySet());
                applyGlobalFilter(q, target);
                return q.select(((Table<ImmutableSpi>)target).fetch(fetcher));
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
                List<ImmutableSpi> targetList = new ArrayList<>(subCollection.size());
                for (Object targetId : subCollection) {
                    ImmutableSpi target = targetMap.get(targetId);
                    if (target != null) {
                        targetList.add(target);
                    }
                }
                fetchedMap.put(e.getKey(), targetList);
            }
        } else if (!noFilter && prop.isReferenceList(TargetLevel.ENTITY)) {
            IdentityHashMap<ImmutableSpi, Object> identityMap = new IdentityHashMap<>();
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                Collection<Object> subCollection = (Collection<Object>) e.getValue();
                for (Object targetId : subCollection) {
                    ImmutableSpi target = targetMap.get(targetId);
                    if (target != null) {
                        identityMap.put(target, e.getKey());
                    }
                }
            }
            for (ImmutableSpi target : targets) {
                Object key = identityMap.get(target);
                if (key != null) {
                    List<Object> targetList = (List<Object>) fetchedMap.get(key);
                    if (targetList == null) {
                        Collection<?> ids = (Collection<?>) map.get(key);
                        targetList = new ArrayList<>(ids.size());
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
        return !remote && (globalFiler != null || propFilter != null || !((ColumnDefinition)storage).isForeignKey());
    }

    public static Connection transientResolverConnection() {
        Connection con = TRANSIENT_RESOLVER_CON_LOCAL.get();
        if (con == null) {
            throw new IllegalStateException("The current thread is not resolving any transient property");
        }
        return con;
    }
}
