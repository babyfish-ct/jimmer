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

    private final ReferenceFetchType defaultFetchType;

    private final int maxJoinFetchDepth;

    private final Caches caches;

    private final Filters filters;

    protected JoinFetchFieldVisitor(JSqlClientImplementor sqlClient) {
        this.defaultFetchType = sqlClient.getDefaultReferenceFetchType();
        this.maxJoinFetchDepth = sqlClient.getMaxJoinFetchDepth();
        this.caches = sqlClient.getCaches();
        this.filters = sqlClient.getFilters();
    }

    public final void visit(Fetcher<?> fetcher) {
        visit0(fetcher, 0);
    }

    private void visit0(Fetcher<?> fetcher, int depth) {
        if (depth >= maxJoinFetchDepth) {
            for (Field field : fetcher.getFieldMap().values()) {
                visit(field);
            }
        } else {
            for (Field field : fetcher.getFieldMap().values()) {
                ReferenceFetchType fetchType = field.getFetchType();
                if (fetchType == ReferenceFetchType.AUTO) {
                    fetchType = defaultFetchType;
                }
                if (fetchType == ReferenceFetchType.SELECT) {
                    visit(field);
                    continue;
                }
                ImmutableProp prop = field.getProp();
                if (!prop.isAssociation(TargetLevel.PERSISTENT) || prop.isReferenceList(TargetLevel.PERSISTENT)) {
                    visit(field);
                    continue;
                }
                Fetcher<?> childFetcher = field.getChildFetcher();
                assert childFetcher != null;
                if (childFetcher.getFieldMap().size() == 1) {
                    if (filters.getTargetFilter(prop) == null) {
                        visit(field);
                        continue;
                    }
                }
                if (fetchType == ReferenceFetchType.JOIN_ALWAYS) {
                    Object enterValue = enter(field);
                    visit0(childFetcher, depth + 1);
                    leave(field, enterValue);
                    continue;
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
                if (propCache == null || caches.getObjectCache(prop.getTargetType()) == null) {
                    Object enterValue = enter(field);
                    visit0(childFetcher, depth + 1);
                    leave(field, enterValue);
                    continue;
                }
                visit(field);
            }
        }
    }

    protected abstract Object enter(Field field);

    protected abstract void leave(Field field, Object enterValue);

    protected void visit(Field field) {}
}
