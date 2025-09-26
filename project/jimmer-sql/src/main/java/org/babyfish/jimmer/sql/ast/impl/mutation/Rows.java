package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.*;
import java.util.function.BiConsumer;

class Rows {

    private Rows() {
    }

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
            if (row.__isLoaded(idPropId)) {
                ids.add(row.__get(idPropId));
            }
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

    static Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> findMapByKeys(
            SaveContext ctx,
            QueryReason queryReason,
            Fetcher<ImmutableSpi> fetcher,
            Collection<? extends ImmutableSpi> rows,
            @Nullable KeyMatcher.Group fixedGroup
    ) {
        Map<KeyMatcher.Group, List<ImmutableSpi>> entityMap = findByKeys(
                ctx,
                queryReason,
                fetcher,
                rows,
                fixedGroup
        );
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
            for (KeyMatcher.Group otherGroup : entityMap.keySet()) {
                if (!group.getName().equals(otherGroup.getName())) {
                    Set<Object> keys = new HashSet<>();
                    for (ImmutableSpi spi : spis) {
                        Object key = Keys.keyOf(spi, otherGroup.getProps());
                        if (!keys.add(key)) {
                            throw ctx.createConflictKey(otherGroup.getProps(), key);
                        }
                    }
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
            // 用于记录缺失的键属性信息
            List<String> missingKeyProps = new ArrayList<>();
            Set<String> processedSpiIds = new HashSet<>();

            for (ImmutableSpi spi : rows) {
                boolean unloaded = false;
                List<String> spiMissingProps = new ArrayList<>();

                for (ImmutableProp keyProp : keyProps) {
                    if (!spi.__isLoaded(keyProp.getId())) {
                        unloaded = true;
                        spiMissingProps.add(keyProp.getName()); // 记录缺失的属性名
                    }
                }

                if (!unloaded) {
                    keys.add(Keys.keyOf(spi, keyProps));
                } else {
                    // 记录当前SPI对象缺失的属性
                    String spiId = spi.__type().getJavaClass().getName();
                    if (!processedSpiIds.contains(spiId)) {
                        processedSpiIds.add(spiId);
                        missingKeyProps.add(String.format(
                                "object[%s]Missing key attributes: %s",
                                spiId,
                                String.join(", ", spiMissingProps)
                        ));
                    }
                }
            }

            if (keys.isEmpty()) {
                // 如果没有有效键且存在缺失属性，抛出详细异常
                if (!missingKeyProps.isEmpty()) {
                    throw new IllegalStateException(
                            "objects lack the necessary key attributes to generate query keys. Details: " +
                                    String.join("; ", missingKeyProps)
                    );
                }
                return Collections.emptyMap();
            }
            return Collections.singletonMap(
                    fixedGroup,
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

    // 辅助方法：获取SPI对象的唯一标识（根据实际情况实现）
    private static String getSpiIdentifier(ImmutableSpi spi) {
        // 这里假设SPI有getId()方法，实际实现需根据ImmutableSpi的结构调整
        try {
            Method getIdMethod = spi.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(spi);
            return id != null ? id.toString() : "未知ID_" + System.identityHashCode(spi);
        } catch (Exception e) {
            // 如果没有getId()方法，使用对象哈希码作为临时标识
            return "对象_" + System.identityHashCode(spi);
        }
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
                            return q.select((Table<ImmutableSpi>) table);
                        }
                        return q.select(
                                ((Table<ImmutableSpi>) table).fetch(fetcher)
                        );
                    }
            ).forUpdate(options.isPessimisticLocked(type)).execute(ctx.con);
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
                                ((Table<ImmutableSpi>) table).fetch(fetcher)
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
