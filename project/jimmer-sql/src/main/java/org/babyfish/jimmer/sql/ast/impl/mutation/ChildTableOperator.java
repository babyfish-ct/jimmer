package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.Converters;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

class ChildTableOperator {

    private final JSqlClient sqlClient;

    private final Connection con;

    private final ImmutableProp parentProp;

    private final MutationCache cache;

    private final MutationTrigger trigger;

    public ChildTableOperator(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp parentProp,
            MutationCache cache,
            MutationTrigger trigger
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.parentProp = parentProp;
        if (trigger != null) {
            this.cache = cache;
            this.trigger = trigger;
        } else {
            this.cache = null;
            this.trigger = null;
        }
    }

    public int setParent(Object parentId, Collection<Object> childIds) {
        if (childIds.isEmpty()) {
            return 0;
        }
        if (trigger != null) {
            return setParentAndPrepareEvents(parentId, childIds);
        }
        return setParentImpl(parentId, childIds);
    }

    private int setParentAndPrepareEvents(Object parentId, Collection<Object> childIds) {
        assert cache != null && trigger != null;
        ImmutableType childType = parentProp.getDeclaringType();
        List<ImmutableSpi> childRows = cache.loadByIds(childType, childIds, con);
        Object currentIdOnly = makeIdOnly(parentProp.getTargetType(), parentId);
        int parentPropId = parentProp.getId();
        List<Object> newChildIds = null;
        for (ImmutableSpi childRow : childRows) {
            Object childId = idOf(childRow);
            Object oldParentId = idOf((ImmutableSpi) childRow.__get(parentPropId));
            Object changedRow = Internal.produce(childType, childRow, (draft) -> {
                ((DraftSpi)draft).__set(parentPropId, currentIdOnly);
            });
            if (Objects.equals(parentId, oldParentId)) {
                if (newChildIds == null) {
                    newChildIds = new ArrayList<>(childIds);
                }
                newChildIds.remove(childId);
            } else {
                trigger.modifyEntityTable(childRow, changedRow);
            }
        }
        if (newChildIds != null) {
            childIds = newChildIds;
        }
        if (childIds.isEmpty()) {
            return 0;
        }
        return setParentImpl(parentId, childIds);
    }

    private int setParentImpl(Object parentId, Collection<Object> childIds) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("update ")
                .sql(parentProp.getDeclaringType().getTableName())
                .sql(" set ")
                .sql(parentProp.<Column>getStorage().getName())
                .sql(" = ");
        if (parentId == null) {
            builder.sql("null");
        } else {
            builder.variable(parentId);
        }
        builder.sql(" where ")
                .sql(parentProp.getDeclaringType().getIdProp().<Column>getStorage().getName())
                .sql(" in(");
        String separator = "";
        for (Object childId : childIds) {
            builder.sql(separator);
            separator = ", ";
            builder.variable(childId);
        }
        builder.sql(")");

        Tuple2<String, List<Object>> sqlResult = builder.build();
        return sqlClient.getExecutor().execute(
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                ExecutionPurpose.MUTATE,
                null,
                PreparedStatement::executeUpdate
        );
    }

    public int unsetParent(Object parentId, Collection<Object> retainedChildIds) {
        if (trigger != null) {
            return unsetParentAndPrepareEvents(Collections.singleton(parentId), retainedChildIds);
        }
        return unsetParentImpl(Collections.singleton(parentId), retainedChildIds);
    }

    public int unsetParents(Collection<Object> parentIds) {
        if (trigger != null) {
            return unsetParentAndPrepareEvents(parentIds, Collections.emptyList());
        }
        return unsetParentImpl(parentIds, Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    private int unsetParentAndPrepareEvents(Collection<Object> parentIds, Collection<Object> retainedChildIds) {
        assert trigger != null;
        int parentPropId = parentProp.getId();
        ImmutableType childType = parentProp.getDeclaringType();
        String parentIdPropName = parentProp.getTargetType().getIdProp().getName();
        String childIdPropName = childType.getIdProp().getName();
        int childIdPropId = childType.getIdProp().getId();
        List<ImmutableSpi> childRows = Internal.requiresNewDraftContext(ctx -> {
            List<ImmutableSpi> list = (List<ImmutableSpi>) Queries
                    .createQuery(sqlClient, parentProp.getDeclaringType(), ExecutionPurpose.MUTATE, true, (q, child) -> {
                        PropExpression<Object> parentIdExpr = child.join(parentProp.getName()).get(parentIdPropName);
                        if (parentIds.size() > 1) {
                            q.where(parentIdExpr.in(parentIds));
                        } else {
                            q.where(parentIdExpr.eq(parentIds.iterator().next()));
                        }
                        if (!retainedChildIds.isEmpty()) {
                            q.where(child.<PropExpression<Object>>get(childIdPropName).notIn(retainedChildIds));
                        }
                        return q.select(child);
                    })
                    .execute(con);
            return ctx.resolveList(list);
        });
        if (childRows.isEmpty()) {
            return 0;
        }
        List<Object> affectedChildIds = new ArrayList<>(childRows.size());
        for (ImmutableSpi childRow : childRows) {
            Object childId = childRow.__get(childIdPropId);
            affectedChildIds.add(childId);
            ImmutableSpi changedRow = (ImmutableSpi) Internal.produce(childType, childRow, draft -> {
                ((DraftSpi)draft).__set(parentPropId, null);
            });
            trigger.modifyEntityTable(childRow, changedRow);
        }
        return setParentImpl(null, affectedChildIds);
    }
    
    private int unsetParentImpl(Collection<Object> parentId, Collection<Object> retainedChildIds) {
        
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("update ")
                .sql(parentProp.getDeclaringType().getTableName())
                .sql(" set ")
                .sql(parentProp.<Column>getStorage().getName())
                .sql(" = null");
        addDetachConditions(builder, parentId, retainedChildIds);

        Tuple2<String, List<Object>> sqlResult = builder.build();
        return sqlClient.getExecutor().execute(
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                ExecutionPurpose.MUTATE,
                null,
                PreparedStatement::executeUpdate
        );
    }

    public List<Object> getDetachedChildIds(Object parentId, Collection<Object> retainedChildIds) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        ImmutableProp idProp = parentProp.getDeclaringType().getIdProp();
        builder
                .sql("select ")
                .sql(idProp.<Column>getStorage().getName())
                .sql(" from ")
                .sql(parentProp.getDeclaringType().getTableName());
        addDetachConditions(builder, Collections.singleton(parentId), retainedChildIds);
        builder.sql(" for update");

        Tuple2<String, List<Object>> sqlResult = builder.build();
        return sqlClient.getExecutor().execute(
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                ExecutionPurpose.MUTATE,
                null,
                stmt -> {
                    List<Object> list = new ArrayList<>();
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Object value = rs.getObject(1);
                            Object id = Converters.tryConvert(value, idProp.getElementClass());
                            if (id == null) {
                                throw new ExecutionException(
                                        "Cannot convert " + value + " to the type of " + idProp
                                );
                            }
                            list.add(id);
                        }
                    }
                    return list;
                }
        );
    }

    private void addDetachConditions(
            SqlBuilder builder,
            Collection<Object> parentIds,
            Collection<Object> retainedChildIds
    ) {
        builder
                .sql(" where ")
                .sql(parentProp.<Column>getStorage().getName());
        if (parentIds.size() > 0) {
            builder.sql(" = ").variable(parentIds.iterator().next());
        } else {
            builder.sql(" in(");
            boolean addComma = false;
            for (Object parentId : parentIds) {
                if (addComma) {
                    builder.sql(", ");
                } else {
                    addComma = true;
                }
                builder.variable(parentId);
            }
            builder.sql(")");
        }
        if (!retainedChildIds.isEmpty()) {
            builder
                    .sql(" and ")
                    .sql(parentProp.getDeclaringType().getIdProp().<Column>getStorage().getName())
                    .sql(" not in(");
            boolean addComma = false;
            for (Object retainedChildId : retainedChildIds) {
                if (addComma) {
                    builder.sql(", ");
                } else {
                    addComma = true;
                }
                builder.variable(retainedChildId);
            }
            builder.sql(")");
        }
    }

    private static ImmutableSpi makeIdOnly(ImmutableType type, Object id) {
        return (ImmutableSpi) Internal.produce(type, null, draft -> {
            ((DraftSpi)draft).__set(type.getIdProp().getId(), id);
        });
    }

    private static Object idOf(ImmutableSpi spi) {
        if (spi == null) {
            return null;
        }
        return spi.__get(spi.__type().getIdProp().getId());
    }
}
