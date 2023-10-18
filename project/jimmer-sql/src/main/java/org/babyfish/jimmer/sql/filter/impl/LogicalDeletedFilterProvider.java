package org.babyfish.jimmer.sql.filter.impl;

import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;

import java.util.*;

public class LogicalDeletedFilterProvider {

    private static final TypeCache<Filter<Props>> DEFAULT_CACHE =
            new TypeCache<>(LogicalDeletedFilterProvider::createDefault, true);

    private static final TypeCache<Filter<Props>> IGNORED_CACHE =
            new TypeCache<>(LogicalDeletedFilterProvider::createIgnored, true);

    private static final TypeCache<Filter<Props>> REVERSED_CACHE =
            new TypeCache<>(LogicalDeletedFilterProvider::createReversed, true);

    private final LogicalDeletedBehavior behavior;

    private final EntityManager entityManager;

    private final String microServiceName;

    public LogicalDeletedFilterProvider(
            LogicalDeletedBehavior behavior,
            EntityManager entityManager,
            String microServiceName
    ) {
        this.behavior = behavior;
        this.entityManager = entityManager;
        this.microServiceName = microServiceName;
    }

    public Filter<Props> get(ImmutableType type) {
        switch (behavior) {
            case IGNORED:
                return IGNORED_CACHE.get(type);
            case REVERSED:
                return REVERSED_CACHE.get(type);
            default:
                return DEFAULT_CACHE.get(type);
        }
    }

    public LogicalDeletedFilterProvider toBehavior(LogicalDeletedBehavior behavior) {
        if (this.behavior == behavior) {
            return this;
        }
        return new LogicalDeletedFilterProvider(behavior, entityManager, microServiceName);
    }

    private static Filter<Props> createDefault(ImmutableType type) {
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        return info != null ? new DefaultFilter(info) : null;
    }

    private static Filter<Props> createIgnored(ImmutableType type) {
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        return info != null ? new IgnoredFilter(info) : null;
    }

    private static Filter<Props> createReversed(ImmutableType type) {
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        return info != null ? new ReversedFilter(info) : null;
    }

    public interface Internal {}

    private static class DefaultFilter implements CacheableFilter<Props>, FilterWrapper, Internal {

        protected final LogicalDeletedInfo info;

        DefaultFilter(LogicalDeletedInfo info) {
            this.info = info;
        }

        @Override
        public Class<?> getFilterType() {
            return this.getClass();
        }

        @Override
        public ImmutableType getImmutableType() {
            return info.getProp().getDeclaringType();
        }

        @Override
        public void filter(FilterArgs<Props> args) {
            Expression<Object> expr = args.getTable().get(info.getProp().getName());
            switch (info.getAction()) {
                case NE:
                    args.where(expr.ne(info.getValue()));
                    break;
                case IS_NOT_NULL:
                    args.where(expr.isNotNull());
                    break;
                case IS_NULL:
                    args.where(expr.isNull());
                    break;
            }
        }

        @Override
        public SortedMap<String, Object> getParameters() {
            return Collections.emptySortedMap();
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            return e.isChanged(info.getProp());
        }
    }

    public static class IgnoredFilter implements Filter<Props>, FilterWrapper, Internal {

        protected final LogicalDeletedInfo info;

        IgnoredFilter(LogicalDeletedInfo info) {
            this.info = info;
        }

        @Override
        public Class<?> getFilterType() {
            return this.getClass();
        }

        @Override
        public ImmutableType getImmutableType() {
            return info.getProp().getDeclaringType();
        }

        @Override
        public void filter(FilterArgs<Props> args) {}
    }

    private static class ReversedFilter implements Filter<Props>, FilterWrapper, Internal {

        protected final LogicalDeletedInfo info;

        ReversedFilter(LogicalDeletedInfo info) {
            this.info = info;
        }

        @Override
        public Class<?> getFilterType() {
            return this.getClass();
        }

        @Override
        public ImmutableType getImmutableType() {
            return info.getProp().getDeclaringType();
        }

        @Override
        public void filter(FilterArgs<Props> args) {
            Expression<Object> expr = args.getTable().get(info.getProp().getName());
            switch (info.getAction()) {
                case NE:
                    args.where(expr.eq(info.getValue()));
                    break;
                case IS_NOT_NULL:
                    args.where(expr.isNull());
                    break;
                case IS_NULL:
                    args.where(expr.isNotNull());
                    break;
            }
        }
    }
}
