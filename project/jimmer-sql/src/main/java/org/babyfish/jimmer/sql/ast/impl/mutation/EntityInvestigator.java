package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.IdOnlyFetchType;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;

import java.sql.BatchUpdateException;
import java.util.*;

class EntityInvestigator {

    private final BatchUpdateException ex;

    private final SaveContext ctx;

    private final Shape shape;

    private final Collection<? extends ImmutableSpi> entities;

    private final boolean updatable;

    private final ImmutableProp idProp;

    private final KeyMatcher keyMatcher;

    private final Map<ImmutableType, Fetcher<ImmutableSpi>> idFetcherMap =
            new HashMap<>();

    private Fetcher<ImmutableSpi> keyFetcher;

    EntityInvestigator(
            BatchUpdateException ex,
            SaveContext ctx,
            Shape shape,
            Collection<? extends ImmutableSpi> entities,
            boolean updatable
    ) {
        this.ex = ex;
        this.ctx = ctx;
        this.shape = shape;
        this.entities = entities;
        this.updatable = updatable;
        this.idProp = ctx.path.getType().getIdProp();
        this.keyMatcher = ctx.options.getKeyMatcher(ctx.path.getType());
    }

    public Exception investigate() {
        if (ctx.options.getSqlClient().getDialect().isBatchUpdateExceptionUnreliable()) {
            Exception translated = translateAll();
            if (translated != null) {
                return translated;
            }
        } else {
            int[] rowCounts = ex.getUpdateCounts();
            int index = 0;
            for (ImmutableSpi entity : entities) {
                if (index >= rowCounts.length || rowCounts[index++] < 0) {
                    Exception translated = translateOne(entity);
                    if (translated != null) {
                        return translated;
                    }
                }
            }
        }
        return ex;
    }

    private Exception translateOne(ImmutableSpi entity) {
        PropId idPropId = idProp.getId();
        if (entity.__isLoaded(idProp.getId()) && !updatable) {
            List<ImmutableSpi> rows = Rows.findByIds(
                    ctx,
                    QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR,
                    idFetcher(null),
                    Collections.singletonList(entity)
            );
            if (!rows.isEmpty()) {
                return ctx.createConflictId(idProp, entity.__get(idPropId));
            }
        }
        for (Map.Entry<String, Set<ImmutableProp>> e : keyMatcher.toMap().entrySet()) {
            String groupName = e.getKey();
            Set<ImmutableProp> keyProps = e.getValue();
            if (!keyProps.isEmpty() &&
                    shape.getGetterMap().keySet().containsAll(keyProps) &&
                    (!updatable || entity.__isLoaded(idPropId))) {
                List<ImmutableSpi> rows = Rows.findByKeys(
                        ctx,
                        QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR,
                        idFetcher(null),
                        Collections.singletonList(entity),
                        keyMatcher.getGroup(groupName)
                ).values().iterator().next();
                if (!rows.isEmpty()) {
                    boolean isSameId = false;
                    if (entity.__isLoaded(idPropId)) {
                        isSameId = entity.__get(idPropId).equals(
                                rows.iterator().next().__get(idPropId)
                        );
                    }
                    if (!isSameId) {
                        return ctx.createConflictKey(
                                keyProps,
                                Keys.keyOf(entity, keyProps)
                        );
                    }
                }
            }
        }
        for (ImmutableProp prop : entity.__type().getProps().values()) {
            PropId propId = prop.getId();
            if (entity.__isLoaded(propId) &&
                    prop.isColumnDefinition() &&
                    prop.isTargetForeignKeyReal(ctx.options.getSqlClient().getMetadataStrategy()) &&
                    prop.isReference(TargetLevel.PERSISTENT) &&
                    !prop.isRemote() &&
                    !ctx.options.isAutoCheckingProp(prop)
            ) {
                Object associatedObject = entity.__get(propId);
                if (associatedObject == null) {
                    continue;
                }
                Object associatedId = ((ImmutableSpi)associatedObject).__get(
                        prop.getTargetType().getIdProp().getId()
                );
                List<ImmutableSpi> rows = Rows.findRows(
                        ctx.prop(prop),
                        QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR,
                        idFetcher(prop.getTargetType()),
                        (q, t) -> {
                            q.where(t.getId().eq(associatedId));
                        }
                );
                if (rows.isEmpty()) {
                    return ctx.prop(prop).createIllegalTargetId(Collections.singleton(associatedId));
                }
            }
        }
        return null;
    }

    private Exception translateAll() {
        if (!updatable && !shape.getIdGetters().isEmpty()) {
            PropId idPropId = idProp.getId();
            Map<Object, ImmutableSpi> rowMap = Rows.findMapByIds(
                    ctx,
                    QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR,
                    idFetcher(null),
                    entities
            );
            for (ImmutableSpi entity : entities) {
                Object id = entity.__get(idPropId);
                if (rowMap.containsKey(id)) {
                    return ctx.createConflictId(idProp, id);
                }
                rowMap.put(id, entity);
            }
        }
        for (Map.Entry<String, Set<ImmutableProp>> e : keyMatcher.toMap().entrySet()) {
            String groupName = e.getKey();
            Set<ImmutableProp> keyProps = e.getValue();
            if (!keyProps.isEmpty() &&
                    shape.getGetterMap().keySet().containsAll(keyProps) &&
                    (!updatable || !shape.getIdGetters().isEmpty())
            ) {
                Map<Object, ImmutableSpi> rowMap = Rows.findMapByKeys(
                        ctx,
                        QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR,
                        keyFetcher(keyProps),
                        entities,
                        keyMatcher.getGroup(groupName)
                ).values().iterator().next();
                PropId idPropId = idProp.getId();
                for (ImmutableSpi entity : entities) {
                    Object key = Keys.keyOf(entity, keyProps);
                    ImmutableSpi row = rowMap.get(key);
                    if (row != null) {
                        boolean isSameId = false;
                        if (entity.__isLoaded(idPropId)) {
                            isSameId = entity.__get(idPropId).equals(row.__get(idPropId));
                        }
                        if (!isSameId) {
                            return ctx.createConflictKey(keyProps, key);
                        }
                    }
                    rowMap.put(key, entity);
                }
            }
        }
        Map<ImmutableProp, Set<Object>> targetIdMultiMap = new LinkedHashMap<>();
        for (ImmutableSpi entity : entities) {
            for (ImmutableProp prop : entity.__type().getProps().values()) {
                PropId propId = prop.getId();
                if (entity.__isLoaded(propId) &&
                        prop.isColumnDefinition() &&
                        prop.isTargetForeignKeyReal(ctx.options.getSqlClient().getMetadataStrategy()) &&
                        prop.isReference(TargetLevel.PERSISTENT) &&
                        !prop.isRemote() &&
                        !ctx.options.isAutoCheckingProp(prop)
                ) {
                    Object associatedObject = entity.__get(propId);
                    if (associatedObject == null) {
                        continue;
                    }
                    Object associatedId = ((ImmutableSpi)associatedObject).__get(
                        prop.getTargetType().getIdProp().getId()
                    );
                    targetIdMultiMap
                            .computeIfAbsent(prop, it -> new LinkedHashSet<>())
                            .add(associatedId);
                }
            }
        }
        for (Map.Entry<ImmutableProp, Set<Object>> e : targetIdMultiMap.entrySet()) {
            ImmutableProp prop = e.getKey();
            Set<Object> associatedIds = e.getValue();
            List<ImmutableSpi> rows = Rows.findRows(
                    ctx.prop(prop),
                    QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR,
                    idFetcher(prop.getTargetType()),
                    (q, t) -> {
                        q.where(t.getId().in(associatedIds));
                    }
            );
            PropId targetIdPropId = prop.getTargetType().getIdProp().getId();
            Set<Object> existingTargetIds = new HashSet<>((rows.size() * 4 + 2) / 3);
            for (ImmutableSpi row : rows) {
                existingTargetIds.add(row.__get(targetIdPropId));
            }
            for (Object associatedId : associatedIds) {
                if (!existingTargetIds.contains(associatedId)) {
                    return ctx.prop(prop).createIllegalTargetId(Collections.singleton(associatedId));
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Fetcher<ImmutableSpi> idFetcher(ImmutableType type) {
        return idFetcherMap.computeIfAbsent(type, t -> {
            if (t == null) {
                t = ctx.path.getType();
            }
            return new FetcherImpl<>((Class<ImmutableSpi>)t.getJavaClass());
        });
    }

    @SuppressWarnings("unchecked")
    private Fetcher<ImmutableSpi> keyFetcher(Set<ImmutableProp> keyProps) {
        Fetcher<ImmutableSpi> keyFetcher = this.keyFetcher;
        if (keyFetcher == null) {
            keyFetcher = new FetcherImpl<>((Class<ImmutableSpi>)ctx.path.getType().getJavaClass());
            for (ImmutableProp keyProp : keyProps) {
                keyFetcher = keyFetcher.add(keyProp.getName(), IdOnlyFetchType.RAW);
            }
            this.keyFetcher = keyFetcher;
        }
        return keyFetcher;
    }
}
