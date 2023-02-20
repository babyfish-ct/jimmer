package org.babyfish.jimmer.sql.filter.impl;

import org.babyfish.jimmer.impl.util.StaticCache;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.BuiltInFilters;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;

import java.util.SortedMap;
import java.util.TreeMap;

public class BuiltinFiltersImpl implements BuiltInFilters {

    private final StaticCache<ImmutableType, Filter<Props>> notDeletedCache =
            new StaticCache<>(this::createNotDeleted, true);

    private final StaticCache<ImmutableType, Filter<Props>> alreadyDeletedCache =
            new StaticCache<>(this::createAlreadyDeleted, true);

    @Override
    public Filter<Props> getDeclaredNotDeletedFilter(ImmutableType immutableType) {
        return notDeletedCache.get(immutableType);
    }

    @Override
    public Filter<Props> getDeclaredNotDeletedFilter(Class<?> type) {
        return getDeclaredNotDeletedFilter(ImmutableType.get(type));
    }

    @Override
    public Filter<Props> getDeclaredAlreadyDeletedFilter(ImmutableType immutableType) {
        return alreadyDeletedCache.get(immutableType);
    }

    @Override
    public Filter<Props> getDeclaredAlreadyDeletedFilter(Class<?> type) {
        return getDeclaredAlreadyDeletedFilter(ImmutableType.get(type));
    }

    private Filter<Props> createNotDeleted(ImmutableType type) {
        LogicalDeletedInfo info = type.getDeclaredLogicalDeletedInfo();
        if (info == null) {
            return null;
        }
        return info.isMultiViewCacheUsed() ?
                new NotDeletedCacheableFilter(info) :
                new NotDeletedFilter(info);
    }

    private Filter<Props> createAlreadyDeleted(ImmutableType type) {
        LogicalDeletedInfo info = type.getDeclaredLogicalDeletedInfo();
        if (info == null) {
            return null;
        }
        return info.isMultiViewCacheUsed() ?
                new AlreadyDeletedCacheableFilter(info) :
                new AlreadyDeletedFilter(info);
    }

    private static class NotDeletedFilter implements Filter<Props>, TypeAware {

        protected final LogicalDeletedInfo info;

        private NotDeletedFilter(LogicalDeletedInfo info) {
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
            PropExpression<Object> expr = args.getTable().get(info.getProp().getName());
            switch (info.getNotDeletedAction()) {
                case NE:
                    if (info.isTwoOptionsOnly()) {
                        args.where(expr.eq(info.getRestoredValue()));
                    } else {
                        args.where(expr.ne(info.getValue()));
                    }
                    break;
                case IS_NOT_NULL:
                    args.where(expr.isNotNull());
                    break;
                case IS_NULL:
                    args.where(expr.isNull());
                    break;
            }
        }
    }

    private static class NotDeletedCacheableFilter extends NotDeletedFilter implements CacheableFilter<Props> {

        private NotDeletedCacheableFilter(LogicalDeletedInfo info) {
            super(info);
        }

        @Override
        public SortedMap<String, Object> getParameters() {
            SortedMap<String, Object> map = new TreeMap<>();
            map.put("logicalDeleted", false);
            return map;
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            return e.getChangedFieldRef(info.getProp().getId()) != null;
        }
    }

    private static class AlreadyDeletedFilter implements Filter<Props>, TypeAware {

        protected final LogicalDeletedInfo info;

        private AlreadyDeletedFilter(LogicalDeletedInfo info) {
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
            PropExpression<Object> expr = args.getTable().get(info.getProp().getName());
            switch (info.getNotDeletedAction()) {
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

    private static class AlreadyDeletedCacheableFilter extends AlreadyDeletedFilter implements CacheableFilter<Props> {

        private AlreadyDeletedCacheableFilter(LogicalDeletedInfo info) {
            super(info);
        }

        @Override
        public SortedMap<String, Object> getParameters() {
            SortedMap<String, Object> map = new TreeMap<>();
            map.put("logicalDeleted", true);
            return map;
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            return e.getChangedFieldRef(info.getProp().getId()) != null;
        }
    }
}
