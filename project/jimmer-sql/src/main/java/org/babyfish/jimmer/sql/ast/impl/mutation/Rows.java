package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.LockMode;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    static Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> findMapByKeys(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Collection<? extends ImmutableSpi> rows
    ) {
        return findMapByKeys(
                ctx,
                queryReason,
                fetcher,
                rows,
                null
        );
    }

    static Map<KeyMatcher.Group, List<ImmutableSpi>> findByKeys(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Collection<? extends ImmutableSpi> rows
    ) {
        return findByKeys(
                ctx,
                queryReason,
                fetcher,
                rows,
                null
        );
    }

    static Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> findMapByKeys(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Collection<? extends ImmutableSpi> rows,
            @Nullable KeyMatcher.Group fixedGroup
    ) {
        Map<KeyMatcher.Group, List<ImmutableSpi>> entityMap = findByKeys(ctx, queryReason, fetcher, rows, fixedGroup);
        if (entityMap.isEmpty()) {
            return new HashMap<>();
        }
        Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> resultMap = new LinkedHashMap<>((entityMap.size() * 4 + 2) / 3);
        for (Map.Entry<KeyMatcher.Group, List<ImmutableSpi>> e : entityMap.entrySet()) {
            KeyMatcher.Group group = e.getKey();
            List<ImmutableSpi> spis = e.getValue();
            Map<Object, ImmutableSpi> keyMap = new LinkedHashMap<>((rows.size() * 4 + 2) / 3);
            for (ImmutableSpi spi : spis) {
                Object key = Keys.keyOf(spi, group.getProps());
                ImmutableSpi conflictEntity = keyMap.put(key, spi);
                if (conflictEntity != null) {
                    throw ctx.createConflictKey(group.getProps(), key);
                }
            }
            resultMap.put(group, keyMap);
        }
        return resultMap;
    }

    static Map<KeyMatcher.Group, List<ImmutableSpi>> findByKeys(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Collection<? extends ImmutableSpi> rows,
            @Nullable KeyMatcher.Group fixedGroup
    ) {
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        KeyMatcher keyMatcher = ctx.options.getKeyMatcher(ctx.path.getType());
        if (keyMatcher.toMap().size() == 1 || fixedGroup != null) {
            if (fixedGroup == null) {
                fixedGroup = keyMatcher.getGroup(
                        keyMatcher.toMap().keySet().iterator().next()
                );
            }
            Set<ImmutableProp> keyProps = fixedGroup.getProps();
            Set<Object> keys = new LinkedHashSet<>((rows.size() * 4 + 2) / 3);
            for (ImmutableSpi spi : rows) {
                keys.add(Keys.keyOf(spi, keyProps));
            }
            if (keys.isEmpty()) {
                return Collections.emptyMap();
            }
            return Collections.singletonMap(
                    keyMatcher.getGroup(
                            keyMatcher.toMap().keySet().iterator().next()
                    ),
                    findByKeys(
                            ctx,
                            queryReason,
                            fetcher,
                            keyProps,
                            keys
                    )
            );
        }
        Map<KeyMatcher.Group, Set<Object>> keyMultiMap = new LinkedHashMap<>();
        for (ImmutableSpi spi : rows) {
            KeyMatcher.Group group = keyMatcher.match(spi);
            if (group == null) {
                ctx.throwNeitherIdNorKey(ctx.path.getType(), keyMatcher.toMap().values().iterator().next());
                continue;
            }
            keyMultiMap
                    .computeIfAbsent(group, it -> new LinkedHashSet<>())
                    .add(Keys.keyOf(spi, group.getProps()));
        }
        if (keyMultiMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<KeyMatcher.Group, List<ImmutableSpi>> resultMap = new LinkedHashMap<>();
        for (Map.Entry<KeyMatcher.Group, Set<Object>> e : keyMultiMap.entrySet()) {
            KeyMatcher.Group group = e.getKey();
            List<ImmutableSpi> list = findByKeys(
                    ctx,
                    queryReason,
                    fetcher,
                    group.getProps(),
                    e.getValue()
            );
            resultMap.put(group, list);
        }
        return resultMap;
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

    private static List<ImmutableSpi> findByKeys(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Set<ImmutableProp> keyProps,
            Set<Object> keys
    ) {
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
}
