package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.LockMode;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SaveException;

import java.sql.Connection;
import java.util.*;
import java.util.function.BiConsumer;

class Rows {

    private Rows() {}

    static Map<Object, ImmutableSpi> findMapByIds(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Collection<? extends ImmutableSpi> rows
    ) {
        List<ImmutableSpi> entities = findByIds(ctx, queryReason, fetcher, rows);
        if (entities.isEmpty()) {
            return new HashMap<>();
        }
        PropId idPropId = ctx.path.getType().getIdProp().getId();
        Map<Object, ImmutableSpi> map = new LinkedHashMap<>((entities.size() * 4 + 2) / 3);
        for (ImmutableSpi entity : entities) {
            map.put(entity.__get(idPropId), entity);
        }
        return map;
    }

    static List<ImmutableSpi> findByIds(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Collection<? extends ImmutableSpi> rows
    ) {
        PropId idPropId = ctx.path.getType().getIdProp().getId();
        Set<Object> ids = new LinkedHashSet<>((rows.size() * 4 + 2) / 3);
        for (ImmutableSpi row : rows) {
            ids.add(row.__get(idPropId));
        }
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return findRows(ctx, queryReason, fetcher, (q, t) -> {
            q.where(t.getId().in(ids));
        });
    }

    static Map<Object, ImmutableSpi> findMapByKeys(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Collection<? extends ImmutableSpi> rows
    ) {
        List<ImmutableSpi> entities = findByKeys(ctx, queryReason, fetcher, rows);
        if (entities.isEmpty()) {
            return new HashMap<>();
        }
        Set<ImmutableProp> keyProps = ctx.options.getKeyProps(ctx.path.getType());
        Map<Object, ImmutableSpi> map = new LinkedHashMap<>((entities.size() * 4 + 2) / 3);
        for (ImmutableSpi entity : entities) {
            Object key = Keys.keyOf(entity, keyProps);
            ImmutableSpi conflictEntity = map.put(Keys.keyOf(entity, keyProps), entity);
            if (conflictEntity != null) {
                throw ctx.createConflictKey(keyProps, key);
            }
        }
        return map;
    }

    static List<ImmutableSpi> findByKeys(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Collection<? extends ImmutableSpi> rows
    ) {
        Set<ImmutableProp> keyProps = ctx.options.getKeyProps(ctx.path.getType());
        Set<Object> keys = new LinkedHashSet<>((rows.size() * 4 + 2) / 3);
        for (ImmutableSpi spi : rows) {
            keys.add(Keys.keyOf(spi, keyProps));
        }
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        return findRows(ctx, queryReason, fetcher, (q, t) -> {
            Expression<Object> keyExpr;
            if (keyProps.size() == 1) {
                ImmutableProp prop = keyProps.iterator().next();
                if (prop.isReference(TargetLevel.PERSISTENT)) {
                    keyExpr = t.getAssociatedId(prop);
                } else {
                    keyExpr = t.get(prop);
                }
            } else {
                Expression<?>[] arr = new Expression[keyProps.size()];
                int index = 0;
                for (ImmutableProp keyProp : keyProps) {
                    Expression<Object> expr;
                    if (keyProp.isReference(TargetLevel.PERSISTENT)) {
                        expr = t.getAssociatedId(keyProp);
                    } else {
                        expr = t.get(keyProp);
                    }
                    arr[index++] = expr;
                }
                keyExpr = Tuples.expressionOf(arr);
            }
            q.where(keyExpr.nullableIn(keys));
        });
    }

    @SuppressWarnings("unchecked")
    static List<ImmutableSpi> findRows(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            BiConsumer<MutableQuery, Table<?>> block
    ) {
        ImmutableType type = ctx.path.getType();
        SaveOptions options = ctx.options;
        return Internal.requiresNewDraftContext(draftContext -> {
            List<ImmutableSpi> list = Queries.createQuery(
                    options.getSqlClient(),
                    type,
                    ExecutionPurpose.command(queryReason),
                    FilterLevel.IGNORE_USER_FILTERS,
                    (q, table) -> {
                        block.accept(q, table);
                        if (ctx.trigger != null) {
                            return q.select((Table<ImmutableSpi>)table);
                        }
                        return q.select(
                                ((Table<ImmutableSpi>)table).fetch(fetcher)
                        );
                    }
            ).forUpdate(options.getLockMode() == LockMode.PESSIMISTIC).execute(ctx.con);
            return draftContext.resolveList(list);
        });
    }

    @SuppressWarnings("unchecked")
    static List<ImmutableSpi> findRows(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            BiConsumer<MutableQuery, Table<?>> block
    ) {
        return Internal.requiresNewDraftContext(draftContext -> {
            List<ImmutableSpi> list = Queries.createQuery(
                    sqlClient,
                    type,
                    ExecutionPurpose.command(queryReason),
                    FilterLevel.IGNORE_USER_FILTERS,
                    (q, table) -> {
                        block.accept(q, table);
                        return q.select(
                                ((Table<ImmutableSpi>)table).fetch(fetcher)
                        );
                    }
            ).execute(con);
            return draftContext.resolveList(list);
        });
    }
}
