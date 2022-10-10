package org.babyfish.jimmer.sql.filter;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.util.StaticCache;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class StatefulFilterManager {

    private final Map<ImmutableType, List<StatefulFilter<Table<?>>>> filterMap;

    private final StaticCache<ImmutableType, StatefulFilter<Table<?>>> cache =
            new StaticCache<>(this::create, true);

    @SuppressWarnings("unchecked")
    public StatefulFilterManager(List<StatefulFilter<?>> filters) {
        Map<ImmutableType, List<StatefulFilter<Table<?>>>> filterMap = new LinkedHashMap<>();
        for (StatefulFilter<?> filter : new LinkedHashSet<>(filters)) {
            if (filter != null) {
                for (ImmutableType immutableType = getImmutableType((StatefulFilter<Table<?>>) filter);
                     immutableType != null;
                     immutableType = immutableType.getSuperType()) {
                    filterMap
                            .computeIfAbsent(immutableType, it -> new ArrayList<>())
                            .add((StatefulFilter<Table<?>>) filter);
                }
            }
        }
        this.filterMap = filterMap;
    }

    public StatefulFilter<Table<?>> get(ImmutableType type) {
        return cache.get(type);
    }

    private StatefulFilter<Table<?>> create(ImmutableType type) {
        List<StatefulFilter<Table<?>>> filters = new ArrayList<>();
        while (type != null) {
            List<StatefulFilter<Table<?>>> list = filterMap.get(type);
            if (list != null) {
                filters.addAll(list);
            }
            type = type.getSuperType();
        }
        if (filters.isEmpty()) {
            return null;
        }
        return new CompositeFilter(filters);
    }

    private ImmutableType getImmutableType(StatefulFilter<Table<?>> filter) {
        Class<?> filterClass = filter.getClass();
        Collection<Type> filterTypeArguments = TypeUtils
                .getTypeArguments(filterClass, StatefulFilter.class)
                .values();
        if (filterTypeArguments.isEmpty()) {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` does not specify the type argument of `" +
                            StatefulFilter.class.getName() +
                            "`"
            );
        }
        Type tableType = filterTypeArguments.iterator().next();
        Class<?> tableClass;
        if (tableType instanceof Class<?>) {
            tableClass = (Class<?>) tableType;
        } else if (tableType instanceof ParameterizedType){
            tableClass = (Class<?>)((ParameterizedType)tableType).getRawType();
        } else {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` is illegal, the type argument of `" +
                            StatefulFilter.class.getName() +
                            "` can only be class of parameterized type"
            );
        }
        if (TableEx.class.isAssignableFrom(tableClass)) {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` is illegal, the type argument of `" +
                            StatefulFilter.class.getName() +
                            "` can not be `TableEx`"
            );
        }
        Collection<Type> tableTypeArguments =TypeUtils
                .getTypeArguments(tableType, Table.class)
                .values();
        if (tableTypeArguments.isEmpty()) {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` does not specify the type argument of `" +
                            Table.class.getName() +
                            "`"
            );
        }
        Type entityType = tableTypeArguments.iterator().next();
        if (!(entityType instanceof Class<?>)) {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` is illegal, the type argument of `" +
                            Table.class.getName() +
                            "` can only be class"
            );
        }
        return ImmutableType.get((Class<?>) entityType);
    }

    private static class CompositeFilter implements StatefulFilter<Table<?>> {

        private final List<StatefulFilter<Table<?>>> filters;

        private CompositeFilter(List<StatefulFilter<Table<?>>> filters) {
            this.filters = filters;
        }

        @Override
        public void filter(FilterArgs<Table<?>> args) {
            for (StatefulFilter<Table<?>> filter : filters) {
                filter.filter(args);
            }
        }

        @Override
        public NavigableMap<String, Object> getParameters() {
            if (filters.size() == 1) {
                return filters.get(0).getParameters();
            }
            NavigableMap<String, Object> map = new TreeMap<>();
            for (StatefulFilter<Table<?>> filter : filters) {
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
            for (StatefulFilter<Table<?>> filter : filters) {
                if (filter.isAffectedBy(e)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isAffectedBy(AssociationEvent e) {
            for (StatefulFilter<Table<?>> filter : filters) {
                if (filter.isAffectedBy(e)) {
                    return true;
                }
            }
            return false;
        }
    }
}
