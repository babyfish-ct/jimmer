package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.*;

class ForeignKeyOperator {

    private final SaveContext ctx;

    private final String tableName;

    private final List<PropertyGetter> foreignKeyGetters;

    private final List<PropertyGetter> idGetters;

    ForeignKeyOperator(SaveContext ctx) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Shape fullShape = Shape.fullOf(sqlClient, ctx.path.getType().getJavaClass());
        this.ctx = ctx;
        this.tableName = ctx.path.getType().getTableName(sqlClient.getMetadataStrategy());
        this.foreignKeyGetters = fullShape.propertyGetters(ctx.backReferenceProp);
        this.idGetters = fullShape.getIdGetters();
    }

    public int disconnectExcept(Iterable<DraftSpi> drafts) {

        PropId parentPropId = ctx.backReferenceProp.getId();
        PropId parentIdPropId = ctx.backReferenceProp.getTargetType().getIdProp().getId();
        PropId idPropId = ctx.path.getType().getIdProp().getId();

        Set<Object> parentIds = new LinkedHashSet<>();
        List<Object> retainedIds;
        if (drafts instanceof Collection<?>) {
            retainedIds = new ArrayList<>(((Collection<DraftSpi>) drafts).size());
        } else {
            retainedIds = new ArrayList<>();
        }

        for (DraftSpi draft : drafts) {
            ImmutableSpi parent = (ImmutableSpi) draft.__get(parentPropId);
            if (parent != null) {
                parentIds.add(parent.__get(parentIdPropId));
            }
            retainedIds.add(draft.__get(idPropId));
        }
        if (parentIds.isEmpty()) {
            return 0;
        }

        if (ctx.trigger == null) {
            return disconnectExceptImpl(parentIds, retainedIds);
        }
        List<ImmutableSpi> detachedRows = findDetachedRows(parentIds, retainedIds);
        if (detachedRows.isEmpty()) {
            return 0;
        }
        List<Object> affectedIds = new ArrayList<>(detachedRows.size());
        for (ImmutableSpi detachedRow : detachedRows) {
            Object childId = detachedRow.__get(idPropId);
            affectedIds.add(childId);
            ImmutableSpi changedRow = (ImmutableSpi) Internal.produce(ctx.path.getType(), detachedRow, draft -> {
                ((DraftSpi)draft).__set(parentPropId, null);
            });
            ctx.trigger.modifyEntityTable(detachedRow, changedRow);
        }
        return disconnectImpl(affectedIds);
    }

    private int disconnectExceptImpl(Set<Object> parentIds, List<Object> retainedIds) {

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        ImmutableProp parentProp = ctx.backReferenceProp;
        MutableUpdateImpl update = new MutableUpdateImpl(sqlClient, ctx.path.getType());
        TableImplementor<?> table = update.getTableImplementor();
        PropExpression<Object> propExpr = table.getAssociatedId(parentProp);
        if (parentProp.isEmbedded(EmbeddedLevel.REFERENCE)) {
            EmbeddedColumns embeddedColumns = parentProp.getTargetType().getIdProp().getStorage(sqlClient.getMetadataStrategy());
            for (EmbeddedColumns.Partial partial : embeddedColumns.getPartialMap().values()) {
                if (!partial.isEmbedded()) {
                    String[] paths = partial.path().split("\\.");
                    PropExpression<Object> subExpr = propExpr;
                    for (String path : paths) {
                        subExpr = ((PropExpression.Embedded<?>)subExpr).get(path);
                    }
                    update.set(subExpr, (Object) null);
                }
            }
        } else {
            update.set(propExpr, (Object) null);
        }
        update.where(propExpr.in(parentIds));
        if (!retainedIds.isEmpty()) {
            update.where(table.getId().notIn(retainedIds));
        }
        return update.execute(ctx.con);
    }

    private int disconnectImpl(List<Object> ids) {

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        ImmutableProp parentProp = ctx.backReferenceProp;
        MutableUpdateImpl update = new MutableUpdateImpl(sqlClient, ctx.path.getType());
        TableImplementor<?> table = update.getTableImplementor();
        update.set(table.getAssociatedId(parentProp), (Object) null);
        update.where(table.getId().notIn(ids));
        return update.execute(ctx.con);
    }

    @SuppressWarnings("unchecked")
    private List<ImmutableSpi> findDetachedRows(
            Set<Object> parentIds,
            List<Object> retainedIds
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        ImmutableProp parentProp = ctx.backReferenceProp;
        MutableRootQueryImpl<Table<Object>> q = new MutableRootQueryImpl<>(
                sqlClient,
                ctx.path.getType(),
                ExecutionPurpose.MUTATE,
                FilterLevel.DEFAULT
        );
        TableImplementor<?> table = q.getTableImplementor();
        q.where(table.getAssociatedId(parentProp).in(parentIds));
        if (!retainedIds.isEmpty()) {
            q.where(table.getId().notIn(retainedIds));
        }
        return Internal.requiresNewDraftContext(draftContext -> {
            List<ImmutableSpi> list = (List<ImmutableSpi>) q.select(table).execute(ctx.con);
            return draftContext.resolveList(list);
        });
    }
}
