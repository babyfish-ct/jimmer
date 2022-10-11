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

    private final Set<Filter<?>> filters;

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
        Map<ImmutableType, List<Filter<Columns>>> filterMap = new LinkedHashMap<>();
        Set<Filter<?>> set = new HashSet<>();
        Set<Class<?>> typeSet = new HashSet<>();
        Set<Filter<?>> disabledSet = new HashSet<>(disabledFilters);
        for (Filter<?> filter : new LinkedHashSet<>(filters)) {
            if (filter != null) {
                set.add(filter);
                collectFilterTypes(filter.getClass(), typeSet);
                for (ImmutableType immutableType = getImmutableType((Filter<Columns>) filter);
                     immutableType != null;
                     immutableType = immutableType.getSuperType()) {
                    filterMap
                            .computeIfAbsent(immutableType, it -> new ArrayList<>())
                            .add((Filter<Columns>) filter);
                }
            }
        }
        disabledSet.retainAll(set);
        this.filterMap = filterMap;
        this.filters = set;
        this.disabledFilters = disabledSet;
        this.filterTypes = typeSet;
        this.disabledFilterTypes = new HashSet<>();
        this.disabledFilterDirectTypes = this.disabledFilterTypes;
        this.filterableReferenceProps = new HashSet<>(filterableReferenceProps);
    }

    private FilterManager(
            Map<ImmutableType, List<Filter<Columns>>> filterMap,
            Set<Filter<?>> filters,
            Set<Filter<?>> disabledFilters,
            Set<Class<?>> filterTypes,
            Set<Class<?>> disabledFilterTypes,
            Set<ImmutableProp> filterableReferenceProps) {
        this.filterMap = filterMap;
        this.filters = filters;
        this.disabledFilters = disabledFilters;
        this.filterTypes = filterTypes;
        this.disabledFilterTypes = disabledFilterTypes;
        this.disabledFilterDirectTypes = disabledFilterDirectTypes();
        this.filterableReferenceProps = filterableReferenceProps;
    }

    public Filter<Columns> get(ImmutableType type) {
        return cache.get(type);
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
                filterMap,
                this.filters,
                disabledSet,
                filterTypes,
                disabledFilterTypes,
                filterableReferenceProps);
    }

    public FilterManager disable(Collection<Filter<?>> filters) {
        if (filters.isEmpty()) {
            return this;
        }
        Set<Filter<?>> disabledSet = new HashSet<>(disabledFilters);
        disabledSet.addAll(filters);
        disabledSet.retainAll(this.filters);
        if (disabledSet.size() == disabledFilters.size()) {
            return this;
        }
        return new FilterManager(
                filterMap,
                this.filters,
                disabledSet,
                filterTypes,
                disabledFilterTypes,
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
        return new FilterManager(
                filterMap,
                filters,
                disabledFilters,
                this.filterTypes,
                disabledTypeSet,
                filterableReferenceProps
        );
    }

    public FilterManager disableByTypes(Collection<Class<?>> filterTypes) {
        if (filterTypes.isEmpty()) {
            return this;
        }
        Set<Class<?>> disabledTypeSet = new HashSet<>(disabledFilterTypes);
        disabledTypeSet.addAll(filterTypes);
        disabledTypeSet.retainAll(this.filterTypes);
        if (disabledTypeSet.size() == disabledFilterTypes.size()) {
            return this;
        }
        return new FilterManager(
                filterMap,
                filters,
                disabledFilters,
                this.filterTypes,
                disabledTypeSet,
                filterableReferenceProps
        );
    }

    public FilterManager addFilterableReferenceProps(Collection<ImmutableProp> props) {
        if (props.isEmpty()) {
            return this;
        }
        Set<ImmutableProp> filterableSet = new HashSet<>(filterableReferenceProps);
        for (ImmutableProp prop : props) {
            if (prop.isReference(TargetLevel.ENTITY) && prop.getStorage() instanceof Column) {
                filterableSet.add(prop);
            } else {
                throw new IllegalArgumentException(
                        "Cannot configure `" +
                                prop +
                                "` as filterable reference property, it is not " +
                                "many-to-one property based on foreign key"
                );
            }
        }
        if (filterableSet.size() == filterableReferenceProps.size()) {
            return this;
        }
        return new FilterManager(
                filterMap,
                filters,
                disabledFilters,
                filterTypes,
                disabledFilterTypes,
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
                filters,
                disabledFilters,
                filterTypes,
                disabledFilterTypes,
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
    
    private Set<Class<?>> disabledFilterDirectTypes() {
        Set<Class<?>> set = new HashSet<>();
        for (Class<?> disabledFilterType : disabledFilterTypes) {
            collectFilterTypes(disabledFilterType, set);
        }
        return set;
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

    private static void collectFilterTypes(Class<?> filterType, Set<Class<?>> filterTypes) {
        if (filterType != null && Filter.class.isAssignableFrom(filterType)) {
            filterTypes.add(filterType);
            collectFilterTypes(filterType.getSuperclass(), filterTypes);
        }
    }

    private void validateFilterableReferenceProps(Collection<ImmutableProp> props) {

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
