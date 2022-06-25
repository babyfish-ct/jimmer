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
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Filter;
import org.babyfish.jimmer.sql.meta.Column;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSingleDataLoader {

    private final SqlClient sqlClient;

    private final Connection con;

    public AbstractSingleDataLoader(
            SqlClient sqlClient,
            Connection con
    ) {
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
            Object key
    ) {}
    
    protected int getLimit() {
        return Integer.MAX_VALUE;
    }
    
    protected int getOffset() {
        return 0;
    }

    public Object load(Object key) {
        if (key == null) {
            return null;
        }
        ImmutableProp prop = getProp();
        if (prop.getStorage() instanceof Column) {
            return loadParent(key);
        }
        if (prop.isEntityList() && prop.getMappedBy() != null && prop.getMappedBy().isReference()) {
            return loadChildren(key);
        }
        Fetcher<?> childFetcher = getChildFetcher();
        if (childFetcher != null && childFetcher.getFieldMap().size() == 1) {
            return loadTargetsWithOnlyId(key);
        }
        return loadTargets(key);
    }

    @SuppressWarnings("unchecked")
    private ImmutableSpi loadParent(Object key) {
        ImmutableProp prop = getProp();
        List<ImmutableSpi> parents = Queries.createQuery(
                sqlClient,
                prop.getTargetType(),
                (q, t) -> {
                    Table<ImmutableSpi> table = (Table<ImmutableSpi>) t;
                    Expression<Object> pk = table.get(
                            prop.getTargetType().getIdProp().getName()
                    );
                    q.where(pk.eq(key));
                    applyFilter(q, table, key);
                    return q.select(
                            table.fetch(
                                    (Fetcher<ImmutableSpi>) getChildFetcher()
                            )
                    );
                }
        ).limit(getLimit(), getOffset()).execute(con);
        return parents.isEmpty() ? null : parents.get(0);
    }

    @SuppressWarnings("unchecked")
    private List<ImmutableSpi> loadChildren(Object key) {
        ImmutableProp prop = getProp();
        return Queries.createQuery(
                sqlClient,
                prop.getTargetType(),
                (q, t) -> {
                    Table<ImmutableSpi> table = (Table<ImmutableSpi>) t;
                    Expression<Object> fk = table
                            .join(prop.getMappedBy().getName())
                            .get(prop.getTargetType().getIdProp().getName());
                    q.where(fk.eq(key));
                    applyFilter(q, table, key);
                    return q.select(
                            table.fetch(
                                    (Fetcher<ImmutableSpi>) getChildFetcher()
                            )
                    );
                }
        ).limit(getLimit(), getOffset()).execute(con);
    }

    @SuppressWarnings("unchecked")
    private List<ImmutableSpi> loadTargetsWithOnlyId(Object key) {
        ImmutableProp prop = getProp();
        AssociationType associationType = AssociationType.of(prop);
        List<Object> targetIds = Queries.createAssociationQuery(
                sqlClient,
                associationType,
                (q, t) -> {
                    Expression<Object> sourceId = t.source().get(prop.getDeclaringType().getIdProp().getName());
                    q.where(sourceId.eq(key));
                    applyFilter(q, (Table<ImmutableSpi>) t.target(), key);
                    return q.select(
                            t.target().<Expression<Object>>get(prop.getTargetType().getIdProp().getName())
                    );
                }
        ).limit(getLimit(), getOffset()).execute(con);
        List<ImmutableSpi> targets = new ArrayList<>(targetIds.size());
        String targetIdPropName = prop.getTargetType().getIdProp().getName();
        for (Object targetId : targetIds) {
            targets.add(
                    (ImmutableSpi) Internal.produce(prop.getTargetType(), null, targetDraft -> {
                        ((DraftSpi) targetDraft).__set(targetIdPropName, targetId);
                    })
            );
        }
        return targets;
    }

    @SuppressWarnings("unchecked")
    private List<ImmutableSpi> loadTargets(Object key) {
        ImmutableProp prop = getProp();
        AssociationType associationType = AssociationType.of(prop);
        return Queries.createAssociationQuery(
                sqlClient,
                associationType,
                (q, t) -> {
                    Expression<Object> sourceId = t.source().get(prop.getDeclaringType().getIdProp().getName());
                    q.where(sourceId.eq(key));
                    applyFilter(q, (Table<ImmutableSpi>) t.target(), key);
                    return q.select(
                            ((Table<ImmutableSpi>) t.target()).fetch(
                                    (Fetcher<ImmutableSpi>) getChildFetcher()
                            )
                    );
                }
        ).limit(getLimit(), getOffset()).execute(con);
    }
}
