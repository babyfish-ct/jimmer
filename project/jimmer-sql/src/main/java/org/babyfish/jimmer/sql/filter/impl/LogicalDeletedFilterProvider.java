package org.babyfish.jimmer.sql.filter.impl;

import org.babyfish.jimmer.impl.util.StaticCache;
import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.Variables;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;

import java.util.*;

public class LogicalDeletedFilterProvider {

    private final TypeCache<Filter<Props>> typeCache =
            new TypeCache<>(this::createTypeProvider, true);

    private final StaticCache<ImmutableProp, Filter<Props>> propCache =
            new StaticCache<>(this::createPropProvider, true);

    private final LogicalDeletedBehavior defaultBehavior;

    private final Map<ImmutableType, LogicalDeletedBehavior> typeBehaviorMap;

    private final Map<ImmutableProp, LogicalDeletedBehavior> propBehaviorMap;

    private final String microServiceName;

    public LogicalDeletedFilterProvider(
            LogicalDeletedBehavior behavior,
            Map<ImmutableType, LogicalDeletedBehavior> typeBehaviorMap,
            Map<ImmutableProp, LogicalDeletedBehavior> propBehaviorMap,
            String microServiceName
    ) {
        this.defaultBehavior = behavior;
        this.typeBehaviorMap = typeBehaviorMap;
        this.propBehaviorMap = propBehaviorMap;
        this.microServiceName = microServiceName;
    }

    public Filter<Props> get(ImmutableType type) {
        return typeCache.get(type);
    }

    public Filter<Props> get(ImmutableProp prop) {
        return propCache.get(prop);
    }

    public LogicalDeletedFilterProvider toBehavior(
            LogicalDeletedBehavior behavior
    ) {
        if (defaultBehavior == behavior) {
            return this;
        }
        return new LogicalDeletedFilterProvider(
                behavior,
                typeBehaviorMap,
                propBehaviorMap,
                microServiceName
        );
    }

    public LogicalDeletedFilterProvider toBehavior(
            ImmutableType type,
            LogicalDeletedBehavior behavior
    ) {
        if (type instanceof AssociationType) {
            return toBehavior(((AssociationType)type).getBaseProp(), behavior);
        }
        if (typeBehaviorMap.get(type) == behavior) {
            return this;
        }
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        if (info == null) {
            throw new IllegalArgumentException(
                    "Cannot set the logical deleted behavior of \"" +
                            type +
                            "\" it does not support logical deletion"
            );
        }
        Map<ImmutableType, LogicalDeletedBehavior> map =
                new HashMap<>(this.typeBehaviorMap);
        map.put(type, behavior);
        return new LogicalDeletedFilterProvider(
                defaultBehavior,
                map,
                propBehaviorMap,
                microServiceName
        );
    }

    public LogicalDeletedFilterProvider toBehavior(
            ImmutableProp prop,
            LogicalDeletedBehavior behavior
    ) {
        if (propBehaviorMap.get(prop) == behavior) {
            return this;
        }
        LogicalDeletedInfo info;
        if (prop.getMappedBy() != null) {
            info = LogicalDeletedInfo.of(prop.getMappedBy());
        } else {
            info = LogicalDeletedInfo.of(prop);
        }
        if (info == null) {
            throw new IllegalArgumentException(
                    "Cannot set the logical deleted behavior of \"" +
                            prop +
                            "\" it is based on middle table with logical deletion"
            );
        }
        Map<ImmutableProp, LogicalDeletedBehavior> map =
                new HashMap<>(this.propBehaviorMap);
        map.put(prop, behavior);
        ImmutableProp oppositeProp = prop.getOpposite();
        if (oppositeProp != null) {
            map.put(oppositeProp, behavior);
        }
        return new LogicalDeletedFilterProvider(
                defaultBehavior,
                typeBehaviorMap,
                map,
                microServiceName
        );
    }

    public LogicalDeletedBehavior getBehavior(ImmutableType type) {
        return typeBehaviorMap.getOrDefault(type, defaultBehavior);
    }

    public LogicalDeletedBehavior getBehavior(ImmutableProp prop) {
        return propBehaviorMap.getOrDefault(prop, defaultBehavior);
    }

    private Filter<Props> createTypeProvider(ImmutableType type) {
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        if (info == null) {
            return null;
        }
        LogicalDeletedBehavior behavior = typeBehaviorMap.getOrDefault(type, defaultBehavior);
        switch (behavior) {
            case IGNORED:
                return new IgnoredFilter(info);
            case REVERSED:
                return new ReversedFilter(info);
            default:
                return new DefaultFilter(info);
        }
    }

    private Filter<Props> createPropProvider(ImmutableProp prop) {
        LogicalDeletedInfo info = LogicalDeletedInfo.of(prop);
        if (info == null) {
            return null;
        }
        LogicalDeletedBehavior behavior = propBehaviorMap.getOrDefault(prop, defaultBehavior);
        switch (behavior) {
            case IGNORED:
                return new IgnoredFilter(info);
            case REVERSED:
                return new ReversedFilter(info);
            default:
                return new DefaultFilter(info);
        }
    }

    public interface Internal {}

    static class DefaultFilter implements CacheableFilter<Props>, FilterWrapper, Internal {

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
            PropExpression<Object> expr = args.getTable().get(info.getProp().getName());
            LogicalDeletedInfo.Action action = info.getAction();
            if (action instanceof LogicalDeletedInfo.Action.Eq) {
                LogicalDeletedInfo.Action.Eq eq = (LogicalDeletedInfo.Action.Eq) action;
                args.where(expr.eq(eq.getValue()));
            } else if (action instanceof LogicalDeletedInfo.Action.Ne) {
                LogicalDeletedInfo.Action.Ne ne = (LogicalDeletedInfo.Action.Ne) action;
                args.where(expr.ne(ne.getValue()));
            } else if (action instanceof LogicalDeletedInfo.Action.IsNull) {
                args.where(expr.isNull());
            } else if (action instanceof LogicalDeletedInfo.Action.IsNotNull) {
                args.where(expr.isNotNull());
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
            PropExpression<Object> expr = args.getTable().get(info.getProp().getName());
            LogicalDeletedInfo.Action action = info.getAction().reversed();
            if (action instanceof LogicalDeletedInfo.Action.Eq) {
                LogicalDeletedInfo.Action.Eq eq = (LogicalDeletedInfo.Action.Eq) action;
                args.where(expr.eq(eq.getValue()));
            } else if (action instanceof LogicalDeletedInfo.Action.Ne) {
                LogicalDeletedInfo.Action.Ne ne = (LogicalDeletedInfo.Action.Ne) action;
                args.where(expr.ne(ne.getValue()));
            } else if (action instanceof LogicalDeletedInfo.Action.IsNull) {
                args.where(expr.isNull());
            } else if (action instanceof LogicalDeletedInfo.Action.IsNotNull) {
                args.where(expr.isNotNull());
            }
        }
    }
}
