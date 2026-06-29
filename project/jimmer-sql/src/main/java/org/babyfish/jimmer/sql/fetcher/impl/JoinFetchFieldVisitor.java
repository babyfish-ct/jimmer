package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.Collections;
import java.util.Map;

public abstract class JoinFetchFieldVisitor {

    private final JSqlClientImplementor sqlClient;

    private final int maxJoinFetchDepth;

    protected JoinFetchFieldVisitor(JSqlClientImplementor sqlClient) {
        this.sqlClient = sqlClient;
        this.maxJoinFetchDepth = sqlClient.getMaxJoinFetchDepth();
    }

    public final void visit(Fetcher<?> fetcher) {
        visit0(fetcher, 0);
    }

    private void visit0(Fetcher<?> fetcher, int depth) {
        if (depth >= maxJoinFetchDepth) {
            for (Field field : fetcher.getFieldMap().values()) {
                visit(field, depth);
            }
        } else {
            for (Field field : fetcher.getFieldMap().values()) {
                if (isJoinField(field, sqlClient)) {
                    Object enterValue = enter(field);
                    visit0(field.getChildFetcher(), depth + 1);
                    leave(field, enterValue);
                } else {
                    visit(field, depth);
                }
            }
        }
        for (Map.Entry<ImmutableType, Fetcher<?>> e : subtypeFetcherMap(fetcher).entrySet()) {
            if (!shouldVisitSubtype(e.getKey(), e.getValue())) {
                continue;
            }
            Object enterValue = enterSubtype(e.getKey());
            visit0(e.getValue(), depth);
            leaveSubtype(e.getKey(), enterValue);
        }
    }

    protected abstract Object enter(Field field);

    protected abstract void leave(Field field, Object enterValue);

    protected Object enterSubtype(ImmutableType subtype) {
        return null;
    }

    protected boolean shouldVisitSubtype(ImmutableType subtype, Fetcher<?> fetcher) {
        return true;
    }

    protected void leaveSubtype(ImmutableType subtype, Object enterValue) {}

    protected void visit(Field field, int depth) {}

    private static Map<ImmutableType, Fetcher<?>> subtypeFetcherMap(Fetcher<?> fetcher) {
        if (fetcher instanceof FetcherImplementor<?>) {
            return ((FetcherImplementor<?>) fetcher).__getSubtypeFetcherMap();
        }
        return Collections.emptyMap();
    }

    public static boolean isJoinField(Field field, JSqlClientImplementor sqlClient) {
        ReferenceFetchType fetchType = field.getFetchType();
        if (fetchType == ReferenceFetchType.AUTO) {
            fetchType = sqlClient.getDefaultReferenceFetchType();
        }
        if (fetchType == ReferenceFetchType.SELECT) {
            return false;
        }
        ImmutableProp prop = field.getProp();
        if (!prop.isAssociation(TargetLevel.PERSISTENT) || prop.isReferenceList(TargetLevel.PERSISTENT)) {
            return false;
        }
        Caches caches = sqlClient.getCaches();
        Filters filters = sqlClient.getFilters();
        Fetcher<?> childFetcher = field.getChildFetcher();
        if (childFetcher == null) {
            return false;
        }
        if (isIdOnlyFetcher(childFetcher)) {
            if (filters.getTargetFilter(prop) == null) {
                return false;
            }
        }
        if (fetchType == ReferenceFetchType.JOIN_ALWAYS) {
            return true;
        }
        Cache<?, ?> propCache = caches.getPropertyCache(prop);
        if (propCache != null) {
            Filter<?> filter = filters.getTargetFilter(prop);
            if (filter != null && (
                    !(filter instanceof CacheableFilter<?>) ||
                            !(propCache instanceof Cache.Parameterized<?, ?>))
            ) {
                propCache = null;
            }
        }
        return propCache == null || caches.getObjectCache(prop.getTargetType()) == null;
    }

    private static boolean isIdOnlyFetcher(Fetcher<?> fetcher) {
        return fetcher.getFieldMap().size() == 1 &&
                subtypeFetcherMap(fetcher).isEmpty();
    }

    public static boolean hasTableFields(
            Fetcher<?> fetcher,
            JSqlClientImplementor sqlClient,
            boolean ignoreIdAndDiscriminator
    ) {
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            if (ignoreIdAndDiscriminator && (prop.isId() || prop.isDiscriminator())) {
                continue;
            }
            if (prop.isColumnDefinition() ||
                    prop.getSqlTemplate() instanceof FormulaTemplate ||
                    isJoinField(field, sqlClient)) {
                return true;
            }
        }
        for (Fetcher<?> subtypeFetcher : subtypeFetcherMap(fetcher).values()) {
            if (hasTableFields(subtypeFetcher, sqlClient, true)) {
                return true;
            }
        }
        return false;
    }
}
