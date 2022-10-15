package org.babyfish.jimmer.sql.filter;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.PropsFor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CachesImpl;
import org.babyfish.jimmer.sql.cache.LocatedCache;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.impl.TypeAwareFilter;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.util.StaticCache;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class FilterManager {

    private final Set<Filter<?>> allFilters;

    private final Set<Filter<?>> disabledFilters;

    private final Map<ImmutableType, List<Filter<Props>>> filterMap;

    private final Map<ImmutableType, List<Filter<Props>>> allCacheableFilterMap;

    private final StaticCache<ImmutableType, Filter<Props>> cache =
            new StaticCache<>(this::create, true);

    private final StaticCache<ImmutableType, List<Filter<Props>>> allCacheableCache =
            new StaticCache<>(this::createAllCacheable, false);

    private JSqlClient sqlClient;

    @SuppressWarnings("unchecked")
    public FilterManager(
            List<Filter<?>> filters,
            Collection<Filter<?>> disabledFilters
    ) {
        this.allFilters = filters(filters);
        this.disabledFilters = disable(null, disabledFilters, this.allFilters);
        this.filterMap = filterMap(this.allFilters, this.disabledFilters);
        this.allCacheableFilterMap = filterMap(
                this.allFilters.stream().filter(it -> it instanceof CacheableFilter<?>).collect(Collectors.toList()),
                Collections.emptyList()
        );
    }

    private FilterManager(
            Set<Filter<?>> filters,
            Set<Filter<?>> disabledFilters,
            Map<ImmutableType, List<Filter<Props>>> filterMap,
            Map<ImmutableType, List<Filter<Props>>> allCacheableFilterMap
    ) {
        this.allFilters = filters;
        this.disabledFilters = disabledFilters;
        this.filterMap = filterMap;
        this.allCacheableFilterMap = allCacheableFilterMap;
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
                allFilters,
                disabledSet,
                filterMap(allFilters, disabledSet),
                allCacheableFilterMap
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
                allFilters,
                disabledSet,
                filterMap(allFilters, disabledSet),
                allCacheableFilterMap
        );
    }

    public FilterManager enableByTypes(Collection<Class<?>> filterTypes) {
        if (filterTypes.isEmpty()) {
            return this;
        }
        List<Filter<?>> deltaSet = new ArrayList<>();
        for (Filter<?> filter : disabledFilters) {
            boolean matched = false;
            for (Class<?> expectedType : filterTypes) {
                if (expectedType.isAssignableFrom(filter.getClass())) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                deltaSet.add(filter);
            }
        }
        return enable(deltaSet);
    }

    public FilterManager disableByTypes(Collection<Class<?>> filterTypes) {
        if (filterTypes.isEmpty()) {
            return this;
        }
        List<Filter<?>> deltaSet = new ArrayList<>();
        for (Filter<?> filter : allFilters) {
            boolean matched = false;
            for (Class<?> expectedType : filterTypes) {
                if (expectedType.isAssignableFrom(filter.getClass())) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                deltaSet.add(filter);
            }
        }
        return disable(deltaSet);
    }

    public FilterManager disableAll() {
        return disable(allFilters);
    }

    public void initialize(JSqlClient sqlClient) {
        if (this.sqlClient != null) {
            throw new IllegalStateException("The filter manager has been initialized");
        }
        if (sqlClient.getEntityManager() == null) {
            for (Filter<?> filter : allFilters) {
                if (filter instanceof CacheableFilter<?>) {
                    throw new IllegalStateException(
                            "The EntityManager of SqlClient must be configured " +
                                    "when \"" +
                                    CacheableFilter.class.getName() +
                                    "\" is used"
                    );
                }
            }
        }
        if (sqlClient.getConnectionManager() == ConnectionManager.ILLEGAL) {
            for (Filter<?> filter : allFilters) {
                if (filter instanceof CacheableFilter<?>) {
                    throw new IllegalStateException(
                            "The ConnectionManager of SqlClient must be configured " +
                                    "when \"" +
                                    CacheableFilter.class.getName() +
                                    "\" is used"
                    );
                }
            }
        }
        this.sqlClient = sqlClient;
        onInitialized();
    }

    @SuppressWarnings("unchecked")
    private Filter<Props> create(ImmutableType type) {
        List<Filter<Props>> filters = new ArrayList<>();
        while (type != null) {
            List<Filter<Props>> list = filterMap.get(type);
            if (list != null) {
                for (Filter<Props> filter : list) {
                    if (!disabledFilters.contains(filter)) {
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
        return new CompositeCacheableFilter(type, (List<CacheableFilter<Props>>)(List<?>)filters);
    }

    @SuppressWarnings("unchecked")
    private List<Filter<Props>> createAllCacheable(ImmutableType type) {
        List<Filter<Props>> filters = new ArrayList<>();
        while (type != null) {
            List<Filter<Props>> list = allCacheableFilterMap.get(type);
            if (list != null) {
                for (Filter<Props> filter : list) {
                    if (!disabledFilters.contains(filter)) {
                        filters.add(filter);
                    }
                }
            }
            type = type.getSuperType();
        }
        return filters;
    }

    private static ImmutableType getImmutableType(Filter<Props> filter) {
        if (filter instanceof TypeAwareFilter) {
            return ((TypeAwareFilter)filter).getImmutableType();
        }
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
        Type propsType = filterTypeArguments.iterator().next();
        Class<?> propsClass;
        if (propsType instanceof Class<?>) {
            propsClass = (Class<?>) propsType;
        } else if (propsType instanceof ParameterizedType){
            propsClass = (Class<?>)((ParameterizedType)propsType).getRawType();
        } else {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` is illegal, the type argument of `" +
                            Filter.class.getName() +
                            "` can only be class of parameterized type"
            );
        }
        if (TableEx.class.isAssignableFrom(propsClass)) {
            throw new IllegalStateException(
                    "`" +
                            filterClass.getName() +
                            "` is illegal, the type argument of `" +
                            Filter.class.getName() +
                            "` can not be `TableEx`"
            );
        }
        if (Table.class.isAssignableFrom(propsClass)) {
            Collection<Type> propsTypeArguments = TypeUtils
                    .getTypeArguments(propsType, Table.class)
                    .values();
            if (propsTypeArguments.isEmpty()) {
                throw new IllegalStateException(
                        "`" +
                                filterClass.getName() +
                                "` does not specify the type argument of `" +
                                Table.class.getName() +
                                "`"
                );
            }
            Type entityType = propsTypeArguments.iterator().next();
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
        } else if (Props.class.isAssignableFrom(propsClass)) {
            PropsFor propsFor = propsClass.getAnnotation(PropsFor.class);
            if (Props.class == propsClass) {
                throw new IllegalStateException(
                        "`" +
                                filterClass.getName() +
                                "` is illegal, its type argument cannot be `" +
                                propsClass.getName() +
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
                                propsClass.getName() +
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

    @SuppressWarnings("unchecked")
    private static Map<ImmutableType, List<Filter<Props>>> filterMap(
            Collection<Filter<?>> filters,
            Collection<Filter<?>> disabledFilters
    ) {
        Map<ImmutableType, List<Filter<Props>>> map = new HashMap<>();
        for (Filter<?> filter : filters) {
            if (filter != null && !disabledFilters.contains(filter)) {
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

        private final ImmutableType type;

        private final List<CacheableFilter<Props>> filters;

        private CompositeCacheableFilter(ImmutableType type, List<CacheableFilter<Props>> filters) {
            this.type = type;
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
            if (type.isAssignableFrom(e.getImmutableType())) {
                for (CacheableFilter<Props> filter : filters) {
                    if (filter.isAffectedBy(e)) {
                        return true;
                    }
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

    private void onInitialized() {
        CachesImpl caches = (CachesImpl) this.sqlClient.getCaches();
        for (Map.Entry<ImmutableProp, LocatedCache<?, ?>> entry : caches.getPropCacheMap().entrySet()) {
            ImmutableProp prop = entry.getKey();
            Cache<?, ?> cache = entry.getValue();
            if (prop.isAssociation(TargetLevel.ENTITY)) {
                List<Filter<Props>> filters = allCacheableCache.get(prop.getTargetType());
                if (!filters.isEmpty()) {
                    sqlClient.getTriggers().addEntityListener(prop.getTargetType(), e -> {
                        onEntityEvent(prop, cache, filters, e);
                    });
                }
            }
        }
    }

    private void onEntityEvent(
            ImmutableProp prop,
            Cache<?, ?> cache,
            List<Filter<Props>> filters,
            EntityEvent<?> e
    ) {
        if (prop.isReferenceList(TargetLevel.ENTITY)) {
            ImmutableProp mappedBy = prop.getMappedBy();
            if (mappedBy != null && mappedBy.getStorage() instanceof Column) {
                if (e.getUnchangedFieldRef(mappedBy) == null) {
                    return;
                }
            }
        }
        boolean affected = false;
        for (Filter<Props> filter : filters) {
            if (filter instanceof CacheableFilter<?> && ((CacheableFilter<?>)filter).isAffectedBy(e)) {
                affected = true;
                break;
            }
        }
        if (affected) {
            removeCache(prop, cache, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void removeCache(ImmutableProp prop, Cache<?, ?> cache, EntityEvent<?> e) {
        ImmutableProp mappedBy = prop.getMappedBy();
        if (mappedBy != null && mappedBy.getStorage() instanceof Column) {
            Ref<Object> ref = e.getUnchangedFieldRef(mappedBy);
            if (ref != null) {
                ImmutableSpi source = (ImmutableSpi) ref.getValue();
                if (source != null) {
                    ImmutableType sourceType = source.__type();
                    Object sourceId = source.__get(sourceType.getIdProp().getId());
                    ((Cache<Object, Object>)cache).delete(sourceId);
                }
            }
        } else {
            ImmutableType declaringType = prop.getDeclaringType();
            List<ImmutableType> sourceTypes =
                    declaringType.isEntity() ?
                            Collections.singletonList(declaringType) :
                            sqlClient.getEntityManager().getImplementationTypes(declaringType);
            String targetIdPropName = prop.getTargetType().getIdProp().getName();
            for (ImmutableType sourceType : sourceTypes) {
                Collection<Object> sourceIds = Queries
                        .createQuery(sqlClient, sourceType, ExecutionPurpose.EVICT_CACHE, true, (q, source) -> {
                            Expression<Object> sourceIdExpr = source.get(sourceType.getIdProp().getName());
                            Expression<Object> targetIdExpr = source.join(prop.getName()).get(targetIdPropName);
                            q.where(targetIdExpr.eq(e.getId()));
                            return q.select(sourceIdExpr);
                        })
                        .distinct()
                        .execute();
                if (!sourceIds.isEmpty()) {
                    ((Cache<Object, Object>)cache).deleteAll(sourceIds);
                }
            }
        }
    }
}
