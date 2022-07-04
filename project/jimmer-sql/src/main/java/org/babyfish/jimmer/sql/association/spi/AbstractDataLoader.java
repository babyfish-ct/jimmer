package org.babyfish.jimmer.sql.association.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.QueryCacheEnvironment;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Filter;
import org.babyfish.jimmer.sql.fetcher.impl.FilterArgsImpl;
import org.babyfish.jimmer.sql.meta.Column;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractDataLoader {

    private final SqlClient sqlClient;

    private final Connection con;

    private final ImmutableProp prop;

    private final Fetcher<ImmutableSpi> fetcher;

    private final Filter<Table<ImmutableSpi>> filter;

    private final ImmutableProp thisIdProp;

    private final ImmutableProp targetIdProp;

    @SuppressWarnings("unchecked")
    protected AbstractDataLoader(
            SqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            Fetcher<?> fetcher,
            Filter<?> filter
    ) {
        if (!prop.isAssociation()) {
            throw new IllegalArgumentException(
                    "\"" + prop + "\" is not association"
            );
        }
        this.sqlClient = sqlClient;
        this.con = con;
        this.prop = prop;
        this.fetcher = (Fetcher<ImmutableSpi>) fetcher;
        this.filter = (Filter<Table<ImmutableSpi>>) filter;
        this.thisIdProp = prop.getDeclaringType().getIdProp();
        this.targetIdProp = prop.getTargetType().getIdProp();
    }

    @SuppressWarnings("unchecked")
    public Map<ImmutableSpi, Object> load(Collection<ImmutableSpi> sources) {
        if (sources.isEmpty()) {
            return Collections.emptyMap();
        }
        if (prop.getStorage() instanceof Column) {
            return (Map<ImmutableSpi, Object>)(Map<?, ?>) loadParents(sources);
        }
        if (prop.isEntityList()) {

        }
        return (Map<ImmutableSpi, Object>)(Map<?, ?>)loadTargetMap(sources);
    }

    @SuppressWarnings("unchecked")
    private Map<ImmutableSpi, ImmutableSpi> loadParents(Collection<ImmutableSpi> sources) {
        Map<ImmutableSpi, Object> sourceFkMap = new IdentityHashMap<>(
                (sources.size() * 4 + 2) / 3
        );
        List<ImmutableSpi> missedFkSources = new ArrayList<>();
        for (ImmutableSpi source : sources) {
            if (source.__isLoaded(prop.getName())) {
                ImmutableSpi target = (ImmutableSpi) source.__get(prop.getName());
                if (target != null) {
                    sourceFkMap.put(source, target.__get(targetIdProp.getName()));
                }
            } else {
                missedFkSources.add(source);
            }
        }
        Cache<Object, Object> fkCache = sqlClient
                .getCaches()
                .getAssociatedIdCache(prop);
        if (fkCache == null) {
            return loadParentsDirectly(sourceFkMap, missedFkSources);
        }
        if (!missedFkSources.isEmpty()) {
            Map<Object, Object> fkMap = fkCache.getAll(
                    toSourceIds(missedFkSources),
                    new QueryCacheEnvironment<>(
                            sqlClient,
                            con,
                            filter,
                            this::queryForeignKeyMap
                    )
            );
            for (ImmutableSpi missedFkSource : missedFkSources) {
                Object fk = fkMap.get(toSourceId(missedFkSource));
                if (fk != null) {
                    sourceFkMap.put(missedFkSource, fk);
                }
            }
        }
        Map<Object, ImmutableSpi> targetMap = Utils.toMap(
                this::toTargetId,
                findTargets(
                        toSourceIds(sourceFkMap.keySet()),
                        new HashSet<>(sourceFkMap.values())
                )
        );
        return Utils.joinMaps(sourceFkMap, targetMap);
    }

    private Map<ImmutableSpi, ImmutableSpi> loadParentsDirectly(
            Map<ImmutableSpi, Object> sourceFkMap,
            List<ImmutableSpi> missedFkSources
    ) {
        Map<ImmutableSpi, ImmutableSpi> map1 = null;
        if (!sourceFkMap.isEmpty()) {
            map1 = Utils.joinMaps(
                    sourceFkMap,
                    Utils.toMap(
                            this::toTargetId,
                            findTargets(
                                    toSourceIds(sourceFkMap.keySet()),
                                    sourceFkMap.values()
                            )
                    )
            );
        }
        Map<ImmutableSpi, ImmutableSpi> map2 = null;
        if (!missedFkSources.isEmpty()) {
            if (filter != null || (fetcher != null && fetcher.getFieldMap().size() > 1)) {
                map2 = Utils.joinCollectionAndMap(
                        missedFkSources,
                        this::toSourceId,
                        Tuple2.toMap(
                                querySourceTargetPairs(toSourceIds(missedFkSources))
                        )
                );
            } else {
                Map<Object, Object> fkMap =
                        queryForeignKeyMap(toSourceIds(missedFkSources));
                map2 = new HashMap<>((missedFkSources.size() * 4 + 2) / 3);
                for (ImmutableSpi missedFkSource : missedFkSources) {
                    ImmutableSpi target = makeIdOnlyTarget(fkMap.get(toSourceId(missedFkSource)));
                    map2.put(missedFkSource, target);
                }
            }
        }
        return Utils.mergeMap(map1, map2);
    }

    private Map<ImmutableSpi, ImmutableSpi> loadTargetMap(Collection<ImmutableSpi> sources) {
        Cache<Object, Object> cache = sqlClient.getCaches().getAssociatedIdCache(prop);
        if (cache == null) {
            return loadTargetMapDirectly(sources);
        }
        List<Object> sourceIds = toSourceIds(sources);
        Map<Object, Object> idMap = Tuple2.toMap(
                querySourceTargetIdPairs(sourceIds)
        );
        Map<Object, ImmutableSpi> targetMap = Utils.toMap(
                this::toTargetId,
                findTargets(sourceIds, idMap.values())
        );
        return Utils.joinCollectionAndMap(
                sources,
                this::toSourceId,
                Utils.joinMaps(idMap, targetMap)
        );
    }

    private Map<ImmutableSpi, ImmutableSpi> loadTargetMapDirectly(Collection<ImmutableSpi> sources) {
        List<Object> sourceIds = toSourceIds(sources);
        Map<Object, ImmutableSpi> targetMap = Tuple2.toMap(
                querySourceTargetPairs(sourceIds)
        );
        return Utils.joinCollectionAndMap(
                sources,
                this::toSourceId,
                targetMap
        );
    }

    private Map<ImmutableSpi, List<ImmutableSpi>> loadTargetMultiMap(Collection<ImmutableSpi> sources) {
        Cache<Object, Object> cache = sqlClient.getCaches().getAssociatedIdCache(prop);
        if (cache == null) {
            return loadTargetMultiMapDirectly(sources);
        }
        List<Object> sourceIds = toSourceIds(sources);
        Map<Object, Object> idMap = Tuple2.toMap(
                querySourceTargetIdPairs(sourceIds)
        );
        Map<Object, List<ImmutableSpi>> targetMap = Utils.toMultiMap(
                this::toTargetId,
                findTargets(sourceIds, idMap.values())
        );
        return Utils.joinCollectionAndMap(
                sources,
                this::toSourceId,
                Utils.joinMaps(idMap, targetMap)
        );
    }

    private Map<ImmutableSpi, List<ImmutableSpi>> loadTargetMultiMapDirectly(Collection<ImmutableSpi> sources) {
        List<Object> sourceIds = toSourceIds(sources);
        Map<Object, List<ImmutableSpi>> targetMap = Tuple2.toMultiMap(
                querySourceTargetPairs(sourceIds)
        );
        return Utils.joinCollectionAndMap(
                sources,
                this::toSourceId,
                targetMap
        );
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> queryForeignKeyMap(Collection<Object> sourceIds) {
        List<Tuple2<Object, Object>> tuples = Queries
                .createQuery(sqlClient, prop.getDeclaringType(), (q, source) -> {
                    Expression<Object> pkExpr = source.get(thisIdProp.getName());
                    Expression<Object> fkExpr = source.join(prop.getName()).get(targetIdProp.getName());
                    q.where(pkExpr.in(sourceIds));
                    q.where(fkExpr.isNotNull());
                    return q.select(pkExpr,fkExpr);
                }).execute(con);
        return Tuple2.toMap(tuples);
    }

    private List<Tuple2<Object, Object>> querySourceTargetIdPairs(Collection<Object> sourceIds) {
        return Queries
                .createQuery(sqlClient, prop.getTargetType(), (q, target) -> {
                    Expression<Object> sourceIdExpr = target
                            .inverseJoin(prop.getDeclaringType().getJavaClass(), prop.getName())
                            .get(thisIdProp.getName());
                    Expression<Object> targetIdExpr = target.get(targetIdProp.getName());
                    q.where(sourceIdExpr.in(sourceIds));
                    applyFilter(q, target, sourceIds);
                    return q.select(sourceIdExpr, targetIdExpr);
                }).execute(con);
    }

    @SuppressWarnings("unchecked")
    private List<Tuple2<Object, ImmutableSpi>> querySourceTargetPairs(
            Collection<Object> sourceIds
    ) {
        return Queries.createQuery(sqlClient, prop.getTargetType(), (q, target) -> {
            Expression<Object> sourceIdExpr = target
                    .inverseJoin(prop.getDeclaringType().getJavaClass(), prop.getName())
                    .get(thisIdProp.getName());
            q.where(sourceIdExpr.in(sourceIds));
            applyFilter(q, target, sourceIds);
            return q.select(sourceIdExpr, ((Table<ImmutableSpi>)target).fetch(fetcher));
        }).execute(con);
    }

    @SuppressWarnings("unchecked")
    private void applyFilter(
            Sortable sortable,
            Table<?> table,
            Collection<Object> keys
    ) {
        if (filter != null) {
            filter.apply(
                    FilterArgsImpl.batchLoaderArgs(
                            sortable,
                            (Table<ImmutableSpi>) table,
                            keys
                    )
            );
        }
    }

    private Object toSourceId(ImmutableSpi source) {
        return source.__get(thisIdProp.getName());
    }

    private List<Object> toSourceIds(Collection<ImmutableSpi> sources) {
        return sources
                .stream()
                .map(this::toSourceId)
                .collect(Collectors.toList());
    }

    private Object toTargetId(ImmutableSpi target) {
        return target.__get(targetIdProp.getName());
    }

    @SuppressWarnings("unchecked")
    private List<ImmutableSpi> findTargets(
            Collection<Object> sourceIdsForFilter,
            Collection<Object> targetIds
    ) {
        if (filter != null) {
            return Queries.createQuery(sqlClient, prop.getTargetType(), (q, target) -> {
                Expression<Object> targetIdExpr = target.get(targetIdProp.getName());
                q.where(targetIdExpr.in(targetIds));
                applyFilter(q, target, sourceIdsForFilter);
                return q.select(((Table<ImmutableSpi>)target).fetch(fetcher));
            }).execute(con);
        }
        if (fetcher != null && fetcher.getFieldMap().size() > 1) {
            return sqlClient.getEntities().findByIds(
                    fetcher,
                    targetIds,
                    con
            );
        }
        return makeIdOnlyTargets(targetIds);
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
            targetDraft.__set(targetIdProp.getName(), id);
        });
    }
}
