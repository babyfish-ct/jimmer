package org.babyfish.jimmer.sql.filter;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.PropsFor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.util.StaticCache;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class FilterManager {

    private final Map<ImmutableType, List<Filter<Props>>> filterMap;

    private final Set<Filter<?>> allFilters;

    private final Set<Filter<?>> disabledFilters;

    private final Set<Class<?>> filterTypes;

    private final Set<Class<?>> disabledFilterTypes;

    private final Set<Class<?>> disabledFilterDirectTypes;

    private final StaticCache<ImmutableType, Filter<Props>> cache =
            new StaticCache<>(this::create, true);

    @SuppressWarnings("unchecked")
    public FilterManager(
            List<Filter<?>> filters,
            Collection<Filter<?>> disabledFilters
    ) {
        this.allFilters = filters(filters);
        this.disabledFilters = disable(null, disabledFilters, this.allFilters);
        this.filterTypes = filterTypes(this.allFilters);
        this.disabledFilterTypes = new HashSet<>();
        this.disabledFilterDirectTypes = filterTypesByTypes(this.disabledFilterTypes);
        this.filterMap = filterMap(this.allFilters, this.disabledFilters, this.disabledFilterDirectTypes);
    }

    private FilterManager(
            Map<ImmutableType, List<Filter<Props>>> filterMap,
            Set<Filter<?>> filters,
            Set<Filter<?>> disabledFilters,
            Set<Class<?>> filterTypes,
            Set<Class<?>> disabledFilterTypes,
            Set<Class<?>> disabledFilterDirectTypes
    ) {
        this.filterMap = filterMap;
        this.allFilters = filters;
        this.disabledFilters = disabledFilters;
        this.filterTypes = filterTypes;
        this.disabledFilterTypes = disabledFilterTypes;
        this.disabledFilterDirectTypes = disabledFilterDirectTypes;
    }

    public Filter<Props> get(ImmutableType type) {
        return cache.get(type);
    }

    public Filter<Props> get(ImmutableProp prop) {
        ImmutableType targetType = prop.getTargetType();
        if (targetType == null) {
            throw new IllegalArgumentException(
                    "`" +
                            prop +
                            "` is not association property"
            );
        }
        return get(targetType);
    }

    public FilterManager enable(Collection<Filter<?>> filters) {
        if (filters.isEmpty()) {
            return this;
        }
        Set<Filter<?>> disabledSet = new HashSet<>(disabledFilters);
        filters.forEach(disabledSet::remove);
        if (disabledSet.size() == disabledFilters.size()) {
            return this;
        }
        return new FilterManager(
                filterMap(allFilters, disabledSet, disabledFilterDirectTypes),
                allFilters,
                disabledSet,
                filterTypes,
                disabledFilterTypes,
                disabledFilterDirectTypes
        );
    }

    public FilterManager disable(Collection<Filter<?>> filters) {
        if (filters.isEmpty()) {
            return this;
        }
        Set<Filter<?>> disabledSet = disable(disabledFilters, filters, this.allFilters);
        if (disabledSet.size() == disabledFilters.size()) {
            return this;
        }
        return new FilterManager(
                this.filterMap(allFilters, disabledSet, disabledFilterDirectTypes),
                allFilters,
                disabledSet,
                filterTypes,
                disabledFilterTypes,
                disabledFilterDirectTypes
        );
    }

    public FilterManager disableByTypes(Collection<Class<?>> filterTypes) {
        if (filterTypes.isEmpty()) {
            return this;
        }
        Set<Class<?>> disabledTypeSet = disable(disabledFilterTypes, filterTypes, this.filterTypes);
        if (disabledTypeSet.size() == disabledFilterTypes.size()) {
            return this;
        }
        Set<Class<?>> disabledDirectTypeSet = filterTypesByTypes(disabledTypeSet);
        return new FilterManager(
                filterMap(allFilters, disabledFilters, disabledDirectTypeSet),
                allFilters,
                disabledFilters,
                this.filterTypes,
                disabledTypeSet,
                disabledDirectTypeSet
        );
    }

    @SuppressWarnings("unchecked")
    private Filter<Props> create(ImmutableType type) {
        List<Filter<Props>> filters = new ArrayList<>();
        while (type != null) {
            List<Filter<Props>> list = filterMap.get(type);
            if (list != null) {
                for (Filter<Props> filter : list) {
                    if (!disabledFilters.contains(filter) &&
                            !disabledFilterTypes.contains(filter.getClass())) {
                        filters.add(filter);
                    }
                }
            }
            type = type.getSuperType();
        }
        if (filters.isEmpty()) {
            return null;
        }
        for (Filter<?> filter : filters) {
            if (!(filter instanceof CacheableFilter<?>)) {
                return new CompositeFilter(filters);        
            }
        }
        return new CompositeCacheableFilter((List<CacheableFilter<Props>>)(List<?>)filters);
    }

    private static ImmutableType getImmutableType(Filter<Props> filter) {
        Class<?> filterClass = filter.getClass();
        Collection<Type> filterTypeArguments = TypeUtils
                .getTypeArguments(filterClass, Filter.class)
                .values();
        if (filterTypeArguments.isEmpty()) {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` does not specify the type argument of `" +
                            Filter.class.getName() +
                            "`"
            );
        }
        Type columnsType = filterTypeArguments.iterator().next();
        Class<?> columnsClass;
        if (columnsType instanceof Class<?>) {
            columnsClass = (Class<?>) columnsType;
        } else if (columnsType instanceof ParameterizedType){
            columnsClass = (Class<?>)((ParameterizedType)columnsType).getRawType();
        } else {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` is illegal, the type argument of `" +
                            Filter.class.getName() +
                            "` can only be class of parameterized type"
            );
        }
        if (TableEx.class.isAssignableFrom(columnsClass)) {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` is illegal, the type argument of `" +
                            Filter.class.getName() +
                            "` can not be `TableEx`"
            );
        }
        if (Table.class.isAssignableFrom(columnsClass)) {
            Collection<Type> columnsTypeArguments = TypeUtils
                    .getTypeArguments(columnsType, Table.class)
                    .values();
            if (columnsTypeArguments.isEmpty()) {
                throw new IllegalStateException(
                        "`" +
                                filterClass.getName() +
                                "` does not specify the type argument of `" +
                                Table.class.getName() +
                                "`"
                );
            }
            Type entityType = columnsTypeArguments.iterator().next();
            if (!(entityType instanceof Class<?>)) {
                throw new IllegalStateException(
                        "`" +
                                filterClass.getName() +
                                "` is illegal, the type argument of `" +
                                Table.class.getName() +
                                "` can only be class or interface"
                );
            }
            return ImmutableType.get((Class<?>) entityType);
        } else if (Props.class.isAssignableFrom(columnsClass)) {
            PropsFor propsFor = columnsClass.getAnnotation(PropsFor.class);
            if (Props.class == columnsClass) {
                throw new IllegalStateException(
                        "`" +
                                filterClass.getName() +
                                "` is illegal, its type argument cannot be `" +
                                columnsClass.getName() +
                                "`"
                );
            }
            if (propsFor == null) {
                throw new IllegalStateException(
                        "`" +
                                filterClass.getName() +
                                "` is illegal, the type argument of `" +
                                Props.class.getName() +
                                "` is `" +
                                columnsClass.getName() +
                                "` which is not decorated by `@" +
                                PropsFor.class.getName() +
                                "`"
                );
            }
            return ImmutableType.get(propsFor.value());
        } else {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` is illegal, its type argument must inherit `" +
                            Props.class.getName() +
                            "`"
            );
        }
    }

    private static Set<Filter<?>> filters(Collection<Filter<?>> filters) {
        Set<Filter<?>> set = new LinkedHashSet<>();
        for (Filter<?> filter : filters) {
            if (filter != null) {
                set.add(filter);
            }
        }
        return set;
    }

    private static Set<Class<?>> filterTypes(Collection<Filter<?>> filters) {
        Set<Class<?>> set = new LinkedHashSet<>();
        for (Filter<?> filter : filters) {
            if (filter != null) {
                collectFilterTypes(filter.getClass(), set);
            }
        }
        return set;
    }

    private static Set<Class<?>> filterTypesByTypes(Collection<Class<?>> filterTypes) {
        Set<Class<?>> set = new LinkedHashSet<>();
        for (Class<?> filterType : filterTypes) {
            if (filterType != null && Filter.class.isAssignableFrom(filterType)) {
                collectFilterTypes(filterType, set);
            }
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private static Map<ImmutableType, List<Filter<Props>>> filterMap(
            Collection<Filter<?>> filters,
            Collection<Filter<?>> disabledFilters,
            Collection<Class<?>> disabledFilterDirectTypes
    ) {
        Map<ImmutableType, List<Filter<Props>>> map = new HashMap<>();
        for (Filter<?> filter : filters) {
            if (filter != null &&
                    !disabledFilters.contains(filter) &&
                    !disabledFilterDirectTypes.contains(filter.getClass())) {
                for (ImmutableType immutableType = getImmutableType((Filter<Props>) filter);
                     immutableType != null;
                     immutableType = immutableType.getSuperType()) {
                    map
                            .computeIfAbsent(immutableType, it -> new ArrayList<>())
                            .add((Filter<Props>) filter);
                }
            }
        }
        return map;
    }

    private static <E> Set<E> disable(Collection<E> base, Collection<E> more, Collection<E> enabled) {
        Set<E> set = base != null ? new HashSet<>(base) : new HashSet<>();
        set.addAll(more);
        set.retainAll(enabled);
        return set;
    }

    private static void collectFilterTypes(Class<?> filterType, Set<Class<?>> filterTypes) {
        if (filterType != null && Filter.class.isAssignableFrom(filterType)) {
            filterTypes.add(filterType);
            collectFilterTypes(filterType.getSuperclass(), filterTypes);
        }
    }

    private static class CompositeFilter implements Filter<Props> {

        private final List<Filter<Props>> filters;

        private CompositeFilter(List<Filter<Props>> filters) {
            this.filters = filters;
        }

        @Override
        public void filter(FilterArgs<Props> args) {
            for (Filter<Props> filter : filters) {
                filter.filter(args);
            }
        }

        @Override
        public String toString() {
            return "CompositeFilter{" +
                    "filters=" + filters +
                    '}';
        }
    }

    private static class CompositeCacheableFilter implements CacheableFilter<Props> {

        private final List<CacheableFilter<Props>> filters;

        private CompositeCacheableFilter(List<CacheableFilter<Props>> filters) {
            this.filters = filters;
        }

        @Override
        public void filter(FilterArgs<Props> args) {
            for (Filter<Props> filter : filters) {
                filter.filter(args);
            }
        }

        @Override
        public NavigableMap<String, Object> getParameters() {
            if (filters.size() == 1) {
                return filters.get(0).getParameters();
            }
            NavigableMap<String, Object> map = new TreeMap<>();
            for (CacheableFilter<Props> filter : filters) {
                for (Map.Entry<String, Object> e : filter.getParameters().entrySet()) {
                    String key = e.getKey();
                    Object value = e.getValue();
                    if (value != null) {
                        Object conflictValue = map.get(key);
                        if (conflictValue != null && !conflictValue.equals(value)) {
                            throw new IllegalStateException(
                                    "Duplicated parameter key `" +
                                            key +
                                            "` in filters: " +
                                            filters
                            );
                        }
                        map.put(key, value);
                    }
                }
            }
            return map;
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            for (CacheableFilter<Props> filter : filters) {
                if (filter.isAffectedBy(e)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isAffectedBy(AssociationEvent e) {
            for (CacheableFilter<Props> filter : filters) {
                if (filter.isAffectedBy(e)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "CompositeCacheableFilter{" +
                    "filters=" + filters +
                    '}';
        }
    }
}
