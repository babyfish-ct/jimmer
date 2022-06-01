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
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.meta.Column;

import java.sql.Connection;
import java.util.*;

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
        if (prop.getMappedBy() != null && prop.getMappedBy().getStorage() instanceof Column) {
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
    private Map<Object, ?> loadChildren(Collection<Object> keys) {
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
}
