package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.meta.Column;

import java.sql.Connection;
import java.util.*;

class BatchDataLoader {

    private SqlClient sqlClient;

    private Connection con;

    private Field field;

    public BatchDataLoader(SqlClient sqlClient, Connection con, Field field) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.field = field;
    }

    public Map<Object, ?> load(Collection<Object> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }
        ImmutableProp prop = field.getProp();
        if (prop.getStorage() instanceof Column) {
            return loadParent(keys);
        }
        if (prop.isEntityList() && prop.getMappedBy() != null && prop.getMappedBy().isReference()) {
            return loadChildren(keys);
        }
        if (field.getChildFetcher() == null || field.getChildFetcher().getFieldMap().size() == 1) {
            return loadTargetsWithOnlyId(keys);
        }
        return loadTargets(keys);
    }

    @SuppressWarnings("unchecked")
    private Map<Object, ImmutableSpi> loadParent(Collection<Object> keys) {
        ImmutableProp prop = field.getProp();
        List<ImmutableSpi> parents = Queries.createQuery(
                sqlClient,
                prop.getTargetType(),
                (q, t) -> {
                    Table<ImmutableSpi> table = (Table<ImmutableSpi>) t;
                    Expression<Object> pk = table.get(
                            prop.getTargetType().getIdProp().getName()
                    );
                    q.where(pk.in(keys));
                    return q.select(
                            table.fetch(
                                    (Fetcher<ImmutableSpi>) field.getChildFetcher()
                            )
                    );
                }
        ).execute(con);

        Map<Object, ImmutableSpi> parentMap = new HashMap<>((parents.size() * 4 + 2) / 3);
        String parentIdPropName = prop.getTargetType().getIdProp().getName();
        for (ImmutableSpi parent : parents) {
            parentMap.put(parent.__get(parentIdPropName), parent);
        }
        return parentMap;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, List<ImmutableSpi>> loadChildren(Collection<Object> keys) {
        ImmutableProp prop = field.getProp();
        List<Tuple2<Object, ImmutableSpi>> tuples = Queries.createQuery(
                sqlClient,
                prop.getTargetType(),
                (q, t) -> {
                    Table<ImmutableSpi> table = (Table<ImmutableSpi>) t;
                    Expression<Object> fk = table
                            .join(prop.getMappedBy().getName())
                            .get(prop.getTargetType().getIdProp().getName());
                    q.where(fk.in(keys));
                    return q.select(
                            fk,
                            table.fetch(
                                    (Fetcher<ImmutableSpi>) field.getChildFetcher()
                            )
                    );
                }
        ).execute(con);
        return Tuple2.toMultiMap(tuples);
    }

    private Map<Object, List<ImmutableSpi>> loadTargetsWithOnlyId(Collection<Object> keys) {
        ImmutableProp prop = field.getProp();
        AssociationType associationType = AssociationType.of(prop);
        List<Tuple2<Object, Object>> tuples = Queries.createAssociationQuery(
                sqlClient,
                associationType,
                (q, t) -> {
                    Expression<Object> sourceId = t.source().get(prop.getDeclaringType().getIdProp().getName());
                    q.where(sourceId.in(keys));
                    return q.select(
                            sourceId,
                            t.target().<Expression<Object>>get(prop.getTargetType().getIdProp().getName())
                    );
                }
        ).execute(con);
        Map<Object, List<ImmutableSpi>> targetMap = new HashMap<>();
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
    }

    @SuppressWarnings("unchecked")
    private Map<Object, List<ImmutableSpi>> loadTargets(Collection<Object> keys) {
        ImmutableProp prop = field.getProp();
        AssociationType associationType = AssociationType.of(prop);
        List<Tuple2<Object, ImmutableSpi>> tuples = Queries.createAssociationQuery(
                sqlClient,
                associationType,
                (q, t) -> {
                    Expression<Object> sourceId = t.source().get(prop.getDeclaringType().getIdProp().getName());
                    q.where(sourceId.in(keys));
                    return q.select(
                            sourceId,
                            ((Table<ImmutableSpi>) t.target()).fetch(
                                    (Fetcher<ImmutableSpi>) field.getChildFetcher()
                            )
                    );
                }
        ).execute(con);
        return Tuple2.toMultiMap(tuples);
    }
}
