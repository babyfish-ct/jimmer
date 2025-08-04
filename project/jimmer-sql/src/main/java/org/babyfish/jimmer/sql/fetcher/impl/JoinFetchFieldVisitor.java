package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.Filters;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

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
    }

    protected abstract Object enter(Field field);

    protected abstract void leave(Field field, Object enterValue);

    protected void visit(Field field, int depth) {}

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
        assert childFetcher != null;
        if (childFetcher.getFieldMap().size() == 1) {
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
}
