package org.babyfish.jimmer.sql.association.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheFilter;
import org.babyfish.jimmer.sql.cache.CacheLoader;
import org.babyfish.jimmer.sql.cache.QueryCacheEnvironment;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.meta.Column;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractBatchDataLoader {

    private final SqlClient sqlClient;

    private final Connection con;

    public AbstractBatchDataLoader(SqlClient sqlClient, Connection con) {
        this.sqlClient = sqlClient;
        this.con = con;
    }

    protected abstract ImmutableProp getProp();

    protected Fetcher<?> getChildFetcher() {
        return null;
    }

    protected CacheFilter getCacheFilter() {
        return null;
    }

    protected void applyFilter(
            Sortable sortable,
            Table<ImmutableSpi> table,
            Collection<Object> keys
    ) {}

    public Map<Object, ?> load(Collection<Object> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }
        ImmutableProp prop = getProp();
        if (prop.getStorage() instanceof Column) {
            return loadParents(keys);
        }
        if (prop.getMappedBy() != null && prop.getMappedBy().getStorage() instanceof Column
        ) {
            return loadChildren(keys);
        }
        Fetcher<?> childFetcher = getChildFetcher();
        if (childFetcher != null && childFetcher.getFieldMap().size() == 1) {
            return loadTargetsWithOnlyId(keys);
        }
        return loadTargets(keys);
    }

    @SuppressWarnings("unchecked")
    private Map<Object, ImmutableSpi> loadParents(Collection<Object> keys) {
        Map<Object, ImmutableSpi> resultMapFromCache = loadParentsFromCache(keys);
        if (resultMapFromCache != null) {
            return resultMapFromCache;
        }
        ImmutableProp prop = getProp();
        List<ImmutableSpi> parents = Queries.createQuery(
                sqlClient,
                prop.getTargetType(),
                (q, t) -> {
                    Table<ImmutableSpi> table = (Table<ImmutableSpi>) t;
                    Expression<Object> pk = table.get(
                            prop.getTargetType().getIdProp().getName()
                    );
                    q.where(pk.in(keys));
                    applyFilter(q, table, keys);
                    return q.select(
                            table.fetch(
                                    (Fetcher<ImmutableSpi>) getChildFetcher()
                            )
                    );
                }
        ).execute(con);

        Map<Object, ImmutableSpi> parentMap = new LinkedHashMap<>((parents.size() * 4 + 2) / 3);
        String parentIdPropName = prop.getTargetType().getIdProp().getName();
        for (ImmutableSpi parent : parents) {
            parentMap.put(parent.__get(parentIdPropName), parent);
        }
        return parentMap;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, ImmutableSpi> loadParentsFromCache(Collection<Object> keys) {
        ImmutableProp prop = getProp();
        Cache<Object, ImmutableSpi> parentCache = sqlClient.getCaches().getObjectCache(prop.getTargetType());
        if (parentCache == null) {
            return null;
        }
        CacheLoader<Object, ImmutableSpi> loader;
        Fetcher<ImmutableSpi> childFetcher = (Fetcher<ImmutableSpi>) getChildFetcher();
        if (childFetcher != null && childFetcher.getFieldMap().size() > 1) {
            return sqlClient.getEntities().findMapByIds(
                    childFetcher,
                    keys,
                    con
            );
        }
        return sqlClient.getEntities().findMapByIds(
                (Class<ImmutableSpi>) prop.getTargetType().getJavaClass(),
                keys,
                con
        );
    }

    @SuppressWarnings("unchecked")
    private Map<Object, ?> loadChildren(Collection<Object> keys) {
        Map<Object, ?> resultMapFromCache = loadListsFromCache(keys);
        if (resultMapFromCache != null) {
            return resultMapFromCache;
        }
        ImmutableProp prop = getProp();
        List<Tuple2<Object, ImmutableSpi>> tuples = Queries.createQuery(
                sqlClient,
                prop.getTargetType(),
                (q, t) -> {
                    Table<ImmutableSpi> table = (Table<ImmutableSpi>) t;
                    Expression<Object> fk = table
                            .join(prop.getMappedBy().getName())
                            .get(prop.getTargetType().getIdProp().getName());
                    q.where(fk.in(keys));
                    applyFilter(q, table, keys);
                    return q.select(
                            fk,
                            table.fetch(
                                    (Fetcher<ImmutableSpi>) getChildFetcher()
                            )
                    );
                }
        ).execute(con);
        return prop.isEntityList() ? Tuple2.toMultiMap(tuples) : Tuple2.toMap(tuples);
    }

    @SuppressWarnings("unchecked")
    private Map<Object, ?> loadTargetsWithOnlyId(Collection<Object> keys) {
        ImmutableProp prop = getProp();
        AssociationType associationType = AssociationType.of(prop);
        List<Tuple2<Object, Object>> tuples = Queries.createAssociationQuery(
                sqlClient,
                associationType,
                (q, t) -> {
                    Expression<Object> sourceId = t.source().get(prop.getDeclaringType().getIdProp().getName());
                    q.where(sourceId.in(keys));
                    applyFilter(q, (Table<ImmutableSpi>) t.target(), keys);
                    return q.select(
                            sourceId,
                            t.target().<Expression<Object>>get(prop.getTargetType().getIdProp().getName())
                    );
                }
        ).execute(con);
        if (prop.isEntityList()) {
            Map<Object, List<ImmutableSpi>> targetMap = new LinkedHashMap<>();
            String targetIdPropName = prop.getTargetType().getIdProp().getName();
            for (Tuple2<Object, Object> tuple : tuples) {
                targetMap
                        .computeIfAbsent(tuple._1(), it -> new ArrayList<>())
                        .add(
                                (ImmutableSpi) Internal.produce(prop.getTargetType(), null, targetDraft -> {
                                    ((DraftSpi) targetDraft).__set(targetIdPropName, tuple._2());
                                })
                        );
            }
            return targetMap;
        } else {
            Map<Object, ImmutableSpi> targetMap = new LinkedHashMap<>();
            String targetIdPropName = prop.getTargetType().getIdProp().getName();
            for (Tuple2<Object, Object> tuple : tuples) {
                targetMap
                        .put(
                                tuple._1(),
                                (ImmutableSpi) Internal.produce(prop.getTargetType(), null, targetDraft -> {
                                    ((DraftSpi) targetDraft).__set(targetIdPropName, tuple._2());
                                })
                        );
            }
            return targetMap;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Object, ?> loadTargets(Collection<Object> keys) {
        ImmutableProp prop = getProp();
        AssociationType associationType = AssociationType.of(prop);
        List<Tuple2<Object, ImmutableSpi>> tuples = Queries.createAssociationQuery(
                sqlClient,
                associationType,
                (q, t) -> {
                    Expression<Object> sourceId = t.source().get(prop.getDeclaringType().getIdProp().getName());
                    q.where(sourceId.in(keys));
                    applyFilter(q, (Table<ImmutableSpi>) t.target(), keys);
                    return q.select(
                            sourceId,
                            ((Table<ImmutableSpi>) t.target()).fetch(
                                    (Fetcher<ImmutableSpi>) getChildFetcher()
                            )
                    );
                }
        ).execute(con);
        return prop.isEntityList() ? Tuple2.toMultiMap(tuples) : Tuple2.toMap(tuples);
    }

    private Map<Object, ?> loadReferencesFromCache(Collection<Object> keys) {
        ImmutableProp prop = getProp();
        Cache<Object, List<Object>> targetIdCache = sqlClient
                .getCaches()
                .getAssociatedIdCache(prop);
        if (targetIdCache == null) {
            return null;
        }
        String targetIdName = prop.getTargetType().getIdProp().getName();
        CacheLoader<Object, Object> loader = it -> {
            List<Tuple2<Object, Object>> tuples = Queries.createQuery(
                    sqlClient,
                    prop.getTargetType(),
                    (q, t) -> {
                        Table<ImmutableSpi> table = (Table<ImmutableSpi>) t;
                        Expression<Object> pk = table
                                .inverseJoin(prop.getDeclaringType().getJavaClass(), prop.getName())
                                .get(prop.getDeclaringType().getIdProp().getName());
                        Expression<Object> targetPk = table
                                .get(targetIdName);
                        q.where(pk.in(keys));
                        applyFilter(q, table, keys);
                        return q.select(pk, targetPk);
                    }
            ).execute(con);
            return Tuple2.toMap(tuples);
        };
        Map<Object, Object> idMap = targetIdCache.getAll(
                keys,
                new QueryCacheEnvironment<>(
                        sqlClient,
                        con,
                        getCacheFilter(),
                        loader
                )
        );
        Collection<Object> targetIds = new LinkedHashSet<>(idMap.values());
        Map<Object, Object> targetMap;
        Fetcher<Object> childFetcher = (Fetcher<Object>) getChildFetcher();
        String targetIdPropName = getProp().getTargetType().getIdProp().getName();
        if (childFetcher != null && childFetcher.getFieldMap().size() > 1) {
            targetMap = sqlClient.getEntities().findMapByIds(
                    childFetcher,
                    targetIds,
                    con
            );
        } else {
            for (Object targetId : targetIds) {
                if (targetId != null) {
                    Object target = Internal.produce(prop.getTargetType(), null, draft -> {
                        DraftSpi spi = (DraftSpi) draft;
                        spi.__set(targetIdPropName, targetId);
                    });
                    targetMap.put(targetId, target);
                }
            }
        }
        return objMultiMap;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, ?> loadListsFromCache(Collection<Object> keys) {
        ImmutableProp prop = getProp();
        Cache<Object, List<Object>> targetIdListCache = sqlClient
                .getCaches()
                .getAssociatedIdListCache(prop);
        if (targetIdListCache == null) {
            return null;
        }
        CacheLoader<Object, List<Object>> loader = it -> {
            List<Tuple2<Object, Object>> tuples = Queries.createQuery(
                    sqlClient,
                    prop.getTargetType(),
                    (q, t) -> {
                        Table<ImmutableSpi> table = (Table<ImmutableSpi>) t;
                        Expression<Object> fk = table
                                .join(prop.getMappedBy().getName())
                                .get(prop.getTargetType().getIdProp().getName());
                        Expression<Object> pk = table
                                .get(prop.getTargetType().getIdProp().getName());
                        q.where(fk.in(keys));
                        applyFilter(q, table, keys);
                        return q.select(fk, pk);
                    }
            ).execute(con);
            return Tuple2.toMultiMap(tuples);
        };
        Map<Object, List<Object>> idMultiMap = targetIdListCache.getAll(
                keys,
                new QueryCacheEnvironment<>(
                        sqlClient,
                        con,
                        getCacheFilter(),
                        loader
                )
        );
        Map<Object, List<Object>> objMultiMap = new LinkedHashMap<>(
                (idMultiMap.size() * 4 + 2) / 3
        );
        Fetcher<Object> childFetcher = (Fetcher<Object>) getChildFetcher();
        String targetIdPropName = getProp().getTargetType().getIdProp().getName();
        for (Map.Entry<Object, List<Object>> e : idMultiMap.entrySet()) {
            Object parentId = e.getKey();
            List<Object> targetIds = e.getValue();
            List<Object> targetObjects;
            if (childFetcher != null && childFetcher.getFieldMap().size() > 1) {
                targetObjects = sqlClient.getEntities().findByIds(
                        childFetcher,
                        targetIds,
                        con
                );
            } else {
                targetObjects = targetIds.stream().map(id ->
                        Internal.produce(prop.getTargetType(), null, draft -> {
                            DraftSpi spi = (DraftSpi) draft;
                            spi.__set(targetIdPropName, id);
                        })
                ).collect(Collectors.toList());
            }
            objMultiMap.put(parentId, targetObjects);
        }
        return objMultiMap;
    }
}
