package org.babyfish.jimmer.sql.filter;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ast.table.Columns;
import org.babyfish.jimmer.sql.ast.table.ColumnsFor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.util.StaticCache;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class FilterManager {

    private final Map<ImmutableType, List<Filter<Columns>>> filterMap;

    private final Set<Filter<?>> allFilters;

    private final Set<Filter<?>> disabledFilters;

    private final Set<Class<?>> filterTypes;

    private final Set<Class<?>> disabledFilterTypes;

    private final Set<Class<?>> disabledFilterDirectTypes;

    private final Set<ImmutableProp> filterableReferenceProps;

    private final StaticCache<ImmutableType, Filter<Columns>> cache =
            new StaticCache<>(this::create, true);

    @SuppressWarnings("unchecked")
    public FilterManager(
            List<Filter<?>> filters,
            Collection<Filter<?>> disabledFilters,
            Collection<ImmutableProp> filterableReferenceProps
    ) {
        validateFilterableReferenceProps(filterableReferenceProps);
        this.allFilters = filters(filters);
        this.disabledFilters = disable(null, disabledFilters, this.allFilters);
        this.filterTypes = filterTypes(this.allFilters);
        this.disabledFilterTypes = new HashSet<>();
        this.disabledFilterDirectTypes = filterTypesByTypes(this.disabledFilterTypes);
        this.filterableReferenceProps = new HashSet<>(filterableReferenceProps);
        this.filterMap = filterMap(this.allFilters, this.disabledFilters, this.disabledFilterDirectTypes);
    }

    private FilterManager(
            Map<ImmutableType, List<Filter<Columns>>> filterMap,
            Set<Filter<?>> filters,
            Set<Filter<?>> disabledFilters,
            Set<Class<?>> filterTypes,
            Set<Class<?>> disabledFilterTypes,
            Set<Class<?>> disabledFilterDirectTypes,
            Set<ImmutableProp> filterableReferenceProps) {
        this.filterMap = filterMap;
        this.allFilters = filters;
        this.disabledFilters = disabledFilters;
        this.filterTypes = filterTypes;
        this.disabledFilterTypes = disabledFilterTypes;
        this.disabledFilterDirectTypes = disabledFilterDirectTypes;
        this.filterableReferenceProps = filterableReferenceProps;
    }

    public Filter<Columns> get(ImmutableType type) {
        return cache.get(type);
    }

    public Filter<Columns> get(ImmutableProp prop) {
        ImmutableType targetType = prop.getTargetType();
        if (targetType == null) {
            throw new IllegalArgumentException(
                    "`" +
                            prop +
                            "` is not association property"
            );
        }
        if (prop.isReference(TargetLevel.ENTITY) && prop.getStorage() instanceof Column) {
            if (!filterableReferenceProps.contains(prop)) {
                return null;
            }
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
                disabledFilterDirectTypes,
                filterableReferenceProps
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
                disabledFilterDirectTypes,
                filterableReferenceProps
        );
    }

    public FilterManager enableByTypes(Collection<Class<?>> filterTypes) {
        if (filterTypes.isEmpty()) {
            return this;
        }
        Set<Class<?>> disabledTypeSet = new HashSet<>(disabledFilterTypes);
        disabledTypeSet.removeAll(filterTypes);
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
                disabledDirectTypeSet,
                filterableReferenceProps
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
                disabledDirectTypeSet,
                filterableReferenceProps
        );
    }

    public FilterManager addFilterableReferenceProps(Collection<ImmutableProp> props) {
        if (props.isEmpty()) {
            return this;
        }
        validateFilterableReferenceProps(props);
        Set<ImmutableProp> filterableSet = new HashSet<>(filterableReferenceProps);
        filterableSet.addAll(props);
        if (filterableSet.size() == filterableReferenceProps.size()) {
            return this;
        }
        return new FilterManager(
                filterMap,
                allFilters,
                disabledFilters,
                filterTypes,
                disabledFilterTypes,
                disabledFilterDirectTypes,
                filterableSet
        );
    }

    public FilterManager removeFilterableReferenceProps(Collection<ImmutableProp> props) {
        if (props.isEmpty()) {
            return this;
        }
        Set<ImmutableProp> filterableSet = new HashSet<>(filterableReferenceProps);
        filterableSet.removeAll(props);
        if (filterableSet.size() == filterableReferenceProps.size()) {
            return this;
        }
        return new FilterManager(
                filterMap,
                allFilters,
                disabledFilters,
                filterTypes,
                disabledFilterTypes,
                disabledFilterDirectTypes,
                filterableSet
        );
    }

    @SuppressWarnings("unchecked")
    private Filter<Columns> create(ImmutableType type) {
        List<Filter<Columns>> filters = new ArrayList<>();
        while (type != null) {
            List<Filter<Columns>> list = filterMap.get(type);
            if (list != null) {
                for (Filter<Columns> filter : list) {
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
        return new CompositeCacheableFilter((List<CacheableFilter<Columns>>)(List<?>)filters);
    }

    private static ImmutableType getImmutableType(Filter<Columns> filter) {
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
        } else if (Columns.class.isAssignableFrom(columnsClass)) {
            ColumnsFor columnsFor = columnsClass.getAnnotation(ColumnsFor.class);
            if (Columns.class == columnsClass) {
                throw new IllegalStateException(
                        "`" +
                                filterClass.getName() +
                                "` is illegal, its type argument cannot be `" +
                                columnsClass.getName() +
                                "`"
                );
            }
            if (columnsFor == null) {
                throw new IllegalStateException(
                        "`" +
                                filterClass.getName() +
                                "` is illegal, the type argument of `" +
                                Columns.class.getName() +
                                "` is `" +
                                columnsClass.getName() +
                                "` which is not decorated by `@" +
                                ColumnsFor.class.getName() +
                                "`"
                );
            }
            return ImmutableType.get(columnsFor.value());
        } else {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` is illegal, its type argument must inherit `" +
                            Columns.class.getName() +
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
    private static Map<ImmutableType, List<Filter<Columns>>> filterMap(
            Collection<Filter<?>> filters,
            Collection<Filter<?>> disabledFilters,
            Collection<Class<?>> disabledFilterDirectTypes
    ) {
        Map<ImmutableType, List<Filter<Columns>>> map = new HashMap<>();
        for (Filter<?> filter : filters) {
            if (filter != null &&
                    !disabledFilters.contains(filter) &&
                    !disabledFilterDirectTypes.contains(filter.getClass())) {
                for (ImmutableType immutableType = getImmutableType((Filter<Columns>) filter);
                     immutableType != null;
                     immutableType = immutableType.getSuperType()) {
                    map
                            .computeIfAbsent(immutableType, it -> new ArrayList<>())
                            .add((Filter<Columns>) filter);
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

    private void validateFilterableReferenceProps(Collection<ImmutableProp> props) {
        for (ImmutableProp prop : props) {
            if (prop == null) {
                throw new IllegalArgumentException("Filterable reference property cannot be null");
            }
            if (!prop.isReference(TargetLevel.ENTITY) || !(prop.getStorage() instanceof Column)) {
                throw new IllegalArgumentException(
                        "Cannot configure `" +
                                prop +
                                "` as filterable reference property, it must be not " +
                                "many-to-one property based on foreign key"
                );
            }
            if (!prop.isNullable()) {
                throw new IllegalArgumentException(
                        "Cannot configure `" +
                                prop +
                                "` as filterable reference property, it must be nullable"
                );
            }
        }
    }

    private static class CompositeFilter implements Filter<Columns> {

        private final List<Filter<Columns>> filters;

        private CompositeFilter(List<Filter<Columns>> filters) {
            this.filters = filters;
        }

        @Override
        public void filter(FilterArgs<Columns> args) {
            for (Filter<Columns> filter : filters) {
                filter.filter(args);
            }
        }
    }

    private static class CompositeCacheableFilter implements CacheableFilter<Columns> {

        private final List<CacheableFilter<Columns>> filters;

        private CompositeCacheableFilter(List<CacheableFilter<Columns>> filters) {
            this.filters = filters;
        }

        @Override
        public void filter(FilterArgs<Columns> args) {
            for (Filter<Columns> filter : filters) {
                filter.filter(args);
            }
        }

        @Override
        public NavigableMap<String, Object> getParameters() {
            if (filters.size() == 1) {
                return filters.get(0).getParameters();
            }
            NavigableMap<String, Object> map = new TreeMap<>();
            for (CacheableFilter<Columns> filter : filters) {
                for (Map.Entry<String, Object> e : filter.getParameters().entrySet()) {
                    if (map.containsKey(e.getKey())) {
                        throw new IllegalStateException(
                                "Duplicated parameter key `" +
                                        e.getKey() +
                                        "` in filters: " + 
                                        filters
                        );
                    }
                    map.put(e.getKey(), e.getValue());
                }
            }
            return map;
        }

        @Override
        public boolean isAffectedBy(EntityEvent<?> e) {
            for (CacheableFilter<Columns> filter : filters) {
                if (filter.isAffectedBy(e)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isAffectedBy(AssociationEvent e) {
            for (CacheableFilter<Columns> filter : filters) {
                if (filter.isAffectedBy(e)) {
                    return true;
                }
            }
            return false;
        }
    }
}
