package org.babyfish.jimmer.sql.filter.impl;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.cache.UsedCache;
import org.babyfish.jimmer.sql.cache.spi.PropCacheInvalidators;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.PropsFor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.cache.CachesImpl;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.event.impl.BackRefIds;
import org.babyfish.jimmer.sql.event.impl.EvictContext;
import org.babyfish.jimmer.sql.filter.*;
import org.babyfish.jimmer.sql.di.AopProxyProvider;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class FilterManager implements Filters {

    private final static ThreadLocal<LinkedList<Filter<?>>> EXECUTING_FILTERS_LOCAL = new ThreadLocal<>();

    private final AopProxyProvider aopProxyProvider;

    private final LogicalDeletedFilterProvider provider;

    private final Set<Filter<?>> allFilters;

    private final Set<Filter<?>> disabledFilters;

    private final Map<String, List<Filter<Props>>> filterMap;

    private final Map<String, List<CacheableFilter<Props>>> allCacheableFilterMap;

    private final TypeCache<Filter<Props>> cache =
            new TypeCache<>(this::create, true);

    private final TypeCache<Filter<Props>> shardingOnlyCache =
            new TypeCache<>(this::createShardingOnly, true);

    private final TypeCache<List<CacheableFilter<Props>>> allCacheableCache =
            new TypeCache<>(this::createAllCacheable, false);

    private JSqlClientImplementor sqlClient;

    @SuppressWarnings("unchecked")
    public FilterManager(
            AopProxyProvider aopProxyProvider,
            LogicalDeletedFilterProvider provider,
            List<Filter<?>> filters,
            Collection<Filter<?>> disabledFilters
    ) {
        this.aopProxyProvider = aopProxyProvider;
        this.provider = provider;
        this.allFilters = standardFilters(filters);
        this.disabledFilters = standardDisabledFilters(null, disabledFilters, this.allFilters);
        this.filterMap = filterMap(this.allFilters, this.disabledFilters);
        this.allCacheableFilterMap = (Map<String, List<CacheableFilter<Props>>>)(Map<?, ?>)filterMap(
                this.allFilters.stream().filter(it -> it instanceof CacheableFilter<?>).collect(Collectors.toList()),
                Collections.emptyList()
        );
    }

    private FilterManager(
            AopProxyProvider aopProxyProvider,
            LogicalDeletedFilterProvider provider,
            Set<Filter<?>> filters,
            Set<Filter<?>> disabledFilters,
            Map<String, List<Filter<Props>>> filterMap,
            Map<String, List<CacheableFilter<Props>>> allCacheableFilterMap
    ) {
        this.aopProxyProvider = aopProxyProvider;
        this.provider = provider;
        this.allFilters = filters;
        this.disabledFilters = disabledFilters;
        this.filterMap = filterMap;
        this.allCacheableFilterMap = allCacheableFilterMap;
    }

    @Override
    public Filter<Props> getFilter(Class<?> type, boolean shardingOnly) {
        return getFilter(ImmutableType.get(type), shardingOnly);
    }

    @Override
    public Filter<Props> getFilter(ImmutableType type, boolean shardingOnly) {
        if (shardingOnly) {
            return shardingOnlyCache.get(type);
        }
        return cache.get(type);
    }

    @Override
    public Filter<Props> getTargetFilter(TypedProp.Association<?, ?> prop, boolean shardingOnly) {
        return getTargetFilter(prop.unwrap(), shardingOnly);
    }

    @Override
    public Filter<Props> getTargetFilter(ImmutableProp prop, boolean shardingOnly) {
        ImmutableType targetType = prop.getTargetType();
        if (targetType == null || targetType.isEmbeddable()) {
            throw new IllegalArgumentException(
                    "`" +
                            prop +
                            "` is not association property"
            );
        }
        return getFilter(targetType, shardingOnly);
    }

    @Override
    public Filter<Props> getLogicalDeletedFilter(ImmutableType type) {
        Filter<Props> filter = provider.get(type);
        if (filter == null) {
            return null;
        }
        if (filter instanceof CacheableFilter<?>) {
            return new ExportedCacheableFilter(type, Collections.singletonList((CacheableFilter<Props>) filter));
        }
        return new ExportedFilter(Collections.singletonList(filter));
    }

    @Override
    public Ref<SortedMap<String, Object>> getParameterMapRef(Class<?> type) {
        return getParameterMapRef(ImmutableType.get(type));
    }

    @Override
    public Ref<SortedMap<String, Object>> getParameterMapRef(ImmutableType type) {
        Filter<Props> filter = getFilter(type);
        if (filter == null) {
            return Ref.empty();
        }
        if (filter instanceof CacheableFilter<?>) {
            return Ref.of(((CacheableFilter<Props>) filter).getParameters());
        }
        return null;
    }

    @Override
    public Ref<SortedMap<String, Object>> getTargetParameterMapRef(ImmutableProp prop) {
        Filter<Props> filter = getTargetFilter(prop);
        if (filter == null) {
            return Ref.empty();
        }
        if (filter instanceof CacheableFilter<?>) {
            return Ref.of(
                    standardParameterMap(((CacheableFilter<Props>) filter).getParameters())
            );
        }
        return null;
    }

    @Override
    public Ref<SortedMap<String, Object>> getTargetParameterMapRef(TypedProp.Association<?, ?> prop) {
        return getTargetParameterMapRef(prop.unwrap());
    }

    public FilterManager setBehavior(LogicalDeletedBehavior behavior) {
        LogicalDeletedFilterProvider newProvider = provider.toBehavior(behavior);
        if (newProvider == provider) {
            return this;
        }
        return new FilterManager(
                aopProxyProvider,
                newProvider,
                allFilters,
                disabledFilters,
                filterMap,
                allCacheableFilterMap
        );
    }

    public FilterManager setBehavior(ImmutableType type, LogicalDeletedBehavior behavior) {
        return new FilterManager(
                aopProxyProvider,
                provider.toBehavior(type, behavior),
                allFilters,
                disabledFilters,
                filterMap,
                allCacheableFilterMap
        );
    }

    public FilterManager setBehavior(Class<?> type, LogicalDeletedBehavior behavior) {
        return setBehavior(ImmutableType.get(type), behavior);
    }

    public FilterManager setBehavior(ImmutableProp prop, LogicalDeletedBehavior behavior) {
        return new FilterManager(
                aopProxyProvider,
                provider.toBehavior(prop, behavior),
                allFilters,
                disabledFilters,
                filterMap,
                allCacheableFilterMap
        );
    }

    public FilterManager setBehavior(TypedProp.Association<?, ?> prop, LogicalDeletedBehavior behavior) {
        return setBehavior(prop.unwrap(), behavior);
    }

    public FilterManager enable(Collection<Filter<?>> filters) {
        if (filters.isEmpty()) {
            return this;
        }
        Set<Filter<?>> disabledSet = new HashSet<>(disabledFilters);
        for (Filter<?> filter : filters) {
            disabledSet.remove(unwrap(filter));
        }
        if (disabledSet.size() == disabledFilters.size()) {
            return this;
        }
        return new FilterManager(
                aopProxyProvider,
                provider,
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
        Set<Filter<?>> disabledSet = standardDisabledFilters(disabledFilters, filters, this.allFilters);
        if (disabledSet.size() == disabledFilters.size()) {
            return this;
        }
        return new FilterManager(
                aopProxyProvider,
                provider,
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
                Class<?> actualType = filter instanceof FilterWrapper ?
                        ((FilterWrapper) filter).getFilterType() :
                        filter.getClass();
                if (expectedType.isAssignableFrom(actualType)) {
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
                Class<?> actualType = filter instanceof FilterWrapper ?
                        ((FilterWrapper) filter).getFilterType() :
                        filter.getClass();
                if (expectedType.isAssignableFrom(actualType)) {
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

    public void initialize(JSqlClientImplementor sqlClient) {
        if (this.sqlClient != null) {
            throw new IllegalStateException("The filter manager has been initialized");
        }
        if (sqlClient.getConnectionManager() == ConnectionManager.EXTERNAL_ONLY) {
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

    public boolean contains(ImmutableType type) {
        for (ImmutableType t : type.getAllTypes()) {
            if (filterMap.containsKey(t.toString())) {
                // No matter enabled or disabled
                return true;
            }
        }
        return false;
    }

    public Set<Filter<?>> getFiltersAffectNullity(ImmutableProp prop) {
        if (!prop.isReference(TargetLevel.ENTITY)) {
            return Collections.emptySet();
        }
        if (prop.getDeclaringType() == prop.getTargetType()) {
            return Collections.emptySet();
        }
        if (provider.get(prop.getTargetType()) != null) {
            return Collections.emptySet();
        }
        Set<Filter<?>> resultFilters = new LinkedHashSet<>();
        Set<Filter<Props>> allowedFilters = new HashSet<>();
        for (ImmutableType t : prop.getDeclaringType().getAllTypes()) {
            List<Filter<Props>> filters = filterMap.get(t.toString());
            if (filters == null) {
                continue;
            }
            for (Filter<Props> filter : filters) {
                if (filter instanceof AssociationIntegrityAssuranceFilter<?>) {
                    allowedFilters.addAll(filters);
                }
            }
        }
        for (ImmutableType t : prop.getTargetType().getAllTypes()) {
            List<Filter<Props>> filters = filterMap.get(t.toString());
            if (filters != null) {
                for (Filter<Props> filter : filters) {
                    if (!allowedFilters.contains(filter)) {
                        resultFilters.add(filter);
                    }
                }
            }
        }
        return resultFilters;
    }

    private Filter<Props> create(ImmutableType type) {
        return create(type, false);
    }

    private Filter<Props> createShardingOnly(ImmutableType type) {
        return create(type, true);
    }

    @SuppressWarnings("unchecked")
    private Filter<Props> create(ImmutableType type, boolean shardingOnly) {
        Set<Filter<Props>> filters = new LinkedHashSet<>();
        if (type != null) {
            Filter<Props> logicalDeletedFilter = provider.get(type);
            if (logicalDeletedFilter != null) {
                filters.add(logicalDeletedFilter);
            }
            for (ImmutableType t : type.getAllTypes()) {
                List<Filter<Props>> list = filterMap.get(t.toString());
                if (list != null) {
                    for (Filter<Props> filter : list) {
                        if ((!shardingOnly || filter instanceof ShardingFilter<?>) &&
                                !disabledFilters.contains(filter)) {
                            filters.add(filter);
                        }
                    }
                }
            }
        }
        if (filters.isEmpty()) {
            return null;
        }
        for (Filter<?> filter : filters) {
            if (!(filter instanceof CacheableFilter<?>)) {
                return new ExportedFilter(filters);
            }
        }
        return new ExportedCacheableFilter(type, (Collection<CacheableFilter<Props>>)(Collection<?>)filters);
    }

    private List<CacheableFilter<Props>> createAllCacheable(ImmutableType type) {
        List<CacheableFilter<Props>> filters = new ArrayList<>();
        for (ImmutableType t : type.getAllTypes()) {
            List<CacheableFilter<Props>> list = allCacheableFilterMap.get(t.toString());
            Filter<Props> logicalDeletedFilter = provider.get(type);
            if (logicalDeletedFilter instanceof CacheableFilter<?>) {
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add((CacheableFilter<Props>) logicalDeletedFilter);
            }
            if (list != null) {
                for (CacheableFilter<Props> filter : list) {
                    if (!disabledFilters.contains(filter)) {
                        filters.add(filter);
                    }
                }
            }
        }
        return filters;
    }

    public static ImmutableType getImmutableType(Filter<?> filter) {

        if (filter instanceof FilterWrapper) {
            return ((FilterWrapper)filter).getImmutableType();
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
            if (Props.class == propsClass) {
                throw new IllegalStateException(
                        "`" +
                                filterClass.getName() +
                                "` is illegal, its type argument cannot be `" +
                                propsClass.getName() +
                                "`"
                );
            }
            PropsFor propsFor = propsClass.getAnnotation(PropsFor.class);
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

    private static Set<Filter<?>> standardFilters(Collection<Filter<?>> filters) {
        Set<Filter<?>> set = new LinkedHashSet<>();
        for (Filter<?> filter : filters) {
            Filter<?> unwrapped = unwrap(filter);
            if (unwrapped != null && !(unwrapped instanceof LogicalDeletedFilterProvider.Internal)) {
                set.add(unwrapped);
            }
        }
        return set;
    }

    private static Filter<?> unwrap(Filter<?> filter) {
        Object o = filter;
        while (o instanceof FilterWrapper) {
            o = ((FilterWrapper)o).unwrap();
            if (o instanceof Filter<?>) {
                filter = (Filter<?>) o;
            }
        }
        return filter;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<Filter<Props>>> filterMap(
            Collection<Filter<?>> filters,
            Collection<Filter<?>> disabledFilters
    ) {
        Map<String, List<Filter<Props>>> map = new HashMap<>();
        for (Filter<?> filter : filters) {
            if (filter != null && !disabledFilters.contains(filter)) {
                ImmutableType immutableType = getImmutableType(filter);
                map
                        .computeIfAbsent(immutableType.toString(), it -> new ArrayList<>())
                        .add((Filter<Props>) filter);
            }
        }
        return map;
    }

    private static Set<Filter<?>> standardDisabledFilters(
            Collection<Filter<?>> base,
            Collection<Filter<?>> more,
            Collection<Filter<?>> all
    ) {
        Set<Filter<?>> set = base != null ? new HashSet<>(base) : new HashSet<>();
        for (Filter<?> filter : more) {
            Filter<?> unwrapped = unwrap(filter);
            if (unwrapped != null && !(unwrapped instanceof LogicalDeletedFilterProvider.Internal)) {
                set.add(unwrapped);
            }
        }
        set.retainAll(all);
        return set;
    }

    private static Collection<?> affectedSourceIds(List<CacheableFilter<Props>> filters, EntityEvent<?> e) {
        if (filters.size() == 1) {
            return filters.get(0).getAffectedSourceIds(e);
        }
        Set<Object> ids = null;
        for (CacheableFilter<?> filter : filters) {
            Collection<?> someIds = filter.getAffectedSourceIds(e);
            if (someIds != null && !someIds.isEmpty()) {
                if (ids == null) {
                    ids = new LinkedHashSet<>();
                }
                for (Object someId : someIds) {
                    if (someId != null) {
                        ids.add(someId);
                    }
                }
            }
        }
        return ids == null || ids.isEmpty() ? null : ids;
    }

    private static Collection<?> affectedSourceIds(List<CacheableFilter<Props>> filters, AssociationEvent e) {
        if (filters.size() == 1) {
            return filters.get(0).getAffectedSourceIds(e);
        }
        Set<Object> ids = null;
        for (CacheableFilter<?> filter : filters) {
            Collection<?> someIds = filter.getAffectedSourceIds(e);
            if (someIds != null && !someIds.isEmpty()) {
                if (ids == null) {
                    ids = new LinkedHashSet<>();
                }
                for (Object someId : someIds) {
                    if (someId != null) {
                        ids.add(someId);
                    }
                }
            }
        }
        return ids == null || ids.isEmpty() ? null : ids;
    }

    private void onInitialized() {
        for (Filter<?> filter : allFilters) {
            if (filter instanceof CacheableFilter<?>) {
                ((CacheableFilter<?>)filter).initialize(sqlClient);
            }
        }
        CachesImpl caches = (CachesImpl) this.sqlClient.getCaches();
        for (Map.Entry<ImmutableType, UsedCache<?, ?>> entry : caches.getObjectCacheMap().entrySet()) {
            ImmutableType type = entry.getKey();
            List<CacheableFilter<Props>> filters = allCacheableCache.get(type);
            if (!filters.isEmpty() &&
                    PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                            filters,
                            EntityEvent.class,
                            aopProxyProvider
                    )
            ) {
                sqlClient.getTriggers().addEntityListener(e -> {
                    EvictContext.execute(() -> {
                        handleOtherChange(type, filters, e);
                    });
                });
            }
            if (!filters.isEmpty() &&
                    PropCacheInvalidators.isGetAffectedSourceIdsOverridden(
                            filters,
                            AssociationEvent.class,
                            aopProxyProvider
                    )
            ) {
                sqlClient.getTriggers().addAssociationListener(e -> {
                    EvictContext.execute(() -> {
                        handleOtherChange(type, filters, e);
                    });
                });
            }
        }
        for (Map.Entry<ImmutableProp, UsedCache<?, ?>> entry : caches.getPropCacheMap().entrySet()) {
            ImmutableProp prop = entry.getKey();
            if (prop.isAssociation(TargetLevel.PERSISTENT)) {
                List<CacheableFilter<Props>> filters = allCacheableCache.get(prop.getTargetType());
                if (!filters.isEmpty()) {
                    sqlClient.getTriggers().addEntityListener(prop.getTargetType(), e -> {
                        handleTargetChange(prop, filters, e);
                    });
                }
            }
        }
    }

    private void handleTargetChange(
            ImmutableProp prop,
            List<CacheableFilter<Props>> filters,
            EntityEvent<?> e
    ) {
        if (e.isEvict()) {
            return;
        }
        boolean affected = false;
        for (CacheableFilter<Props> filter : filters) {
            if (filter.isAffectedBy(e)) {
                affected = true;
                break;
            }
        }
        if (!affected) {
            return;
        }
        if (prop.isAssociation(TargetLevel.PERSISTENT)) {
            ImmutableProp mappedBy = prop.getMappedBy();
            if (mappedBy != null && mappedBy.isColumnDefinition()) {
                ImmutableSpi oe = (ImmutableSpi) e.getOldEntity();
                ImmutableSpi ne = (ImmutableSpi) e.getNewEntity();
                if (oe == null || ne == null) {
                    return;
                }
                Ref<ImmutableSpi> unchangedParentRef = e.getUnchangedRef(mappedBy);
                if (unchangedParentRef == null) {
                    return;
                }
                ImmutableSpi unchangedParent = unchangedParentRef.getValue();
                if (unchangedParent == null) {
                    return;
                }
                ImmutableProp parentIdProp = mappedBy.getTargetType().getIdProp();
                Object parentId = ImmutableObjects.get(unchangedParent, parentIdProp);
                sqlClient.getTriggers().fireAssociationEvict(prop, parentId, e.getConnection(), e.getReason());
                return;
            }
        }
        List<?> backRefIds = BackRefIds.findBackRefIds(sqlClient, prop, e.getId(), e.getConnection());
        for (Object backRefId : backRefIds) {
            sqlClient.getTriggers().fireAssociationEvict(prop, backRefId, e.getConnection(), e.getReason());
        }
    }

    private void handleOtherChange(
            ImmutableType type,
            List<CacheableFilter<Props>> filters,
            EntityEvent<?> e
    ) {
        EvictContext ctx = EvictContext.get();
        if (ctx != null) {
            ctx.add(e.getImmutableType(), e.getId());
        }
        Collection<?> sourceIds = affectedSourceIds(filters, e);
        if (sourceIds != null) {
            Triggers triggers = sqlClient.getTriggers();
            for (Object sourceId : sourceIds) {
                triggers.fireEntityEvict(type, sourceId, e.getConnection());
            }
        }
    }

    private void handleOtherChange(
            ImmutableType type,
            List<CacheableFilter<Props>> filters,
            AssociationEvent e
    ) {
        EvictContext ctx = EvictContext.get();
        if (ctx != null) {
            ctx.add(e.getImmutableProp(), e.getSourceId());
        }
        if (ctx != null && !e.isEvict()) {
            ctx.disable(e.getImmutableProp());
        }
        Collection<?> sourceIds = affectedSourceIds(filters, e);
        if (sourceIds != null) {
            Triggers triggers = sqlClient.getTriggers();
            for (Object sourceId : sourceIds) {
                triggers.fireEntityEvict(type, sourceId, e.getConnection());
            }
        }
    }

    public Set<ImmutableType> getAffectedTypes(Collection<ImmutableType> allTypes) {
        Set<ImmutableType> affectTypes = new HashSet<>();
        for (ImmutableType type : allTypes) {
            for (ImmutableType upcastType : type.getAllTypes()) {
                if (!affectTypes.contains(upcastType) && filterMap.containsKey(upcastType.toString())) {
                    affectTypes.add(type);
                    break;
                }
            }
        }
        return affectTypes;
    }

    @Override
    public @NotNull LogicalDeletedBehavior getBehavior(ImmutableType type) {
        return provider.getBehavior(type);
    }

    @Override
    public @NotNull LogicalDeletedBehavior getBehavior(ImmutableProp prop) {
        return provider.getBehavior(prop);
    }

    public static Filter<?> currentFilter() {
        LinkedList<Filter<?>> executingFilters = EXECUTING_FILTERS_LOCAL.get();
        return executingFilters != null ? executingFilters.peek() : null;
    }

    public static void executing(Filter<?> filter, Runnable block) {
        if (filter == null) {
            block.run();
            return;
        }
        if (filter instanceof Exported) {
            throw new IllegalArgumentException("The filter cannot be exported filter");
        }
        LinkedList<Filter<?>> executingFilters = EXECUTING_FILTERS_LOCAL.get();
        if (executingFilters == null) {
            executingFilters = new LinkedList<>();
            executingFilters.add(filter);
            EXECUTING_FILTERS_LOCAL.set(executingFilters);
            try {
                block.run();
            } finally {
                EXECUTING_FILTERS_LOCAL.remove();
            }
        } else {
            if (executingFilters.contains(filter)) {
                throw new IllegalStateException(
                        "A dead recursion was discovered during the filter execution process, " +
                                "where the filter \"" +
                                filter +
                                "\" to be executed is the same as the filters \"" +
                                executingFilters +
                                "\" currently being executed in the context."
                );
            }
            executingFilters.push(filter);
            try {
                block.run();
            } finally {
                executingFilters.pop();
            }
        }
    }

    private static class ExportedFilter implements Filter<Props>, Exported {

        private final List<Filter<Props>> filters;

        private ExportedFilter(Collection<Filter<Props>> filters) {
            this.filters = new ArrayList<>(filters);
        }

        @Override
        public void filter(FilterArgs<Props> args) {
            for (Filter<Props> filter : filters) {
                executing(filter, () -> {
                    filter.filter(args);
                });
            }
        }

        @Override
        public boolean isLogicalDeletedFilter() {
            return filters.size() == 1 && filters.get(0) instanceof LogicalDeletedFilterProvider.DefaultFilter;
        }

        @Override
        public String toString() {
            return "ExportedFilter{" +
                    "filters=" + filters +
                    '}';
        }
    }

    private static class ExportedCacheableFilter implements CacheableFilter<Props>, Exported {

        private final ImmutableType type;

        private final List<CacheableFilter<Props>> filters;

        private ExportedCacheableFilter(ImmutableType type, Collection<CacheableFilter<Props>> filters) {
            this.type = type;
            this.filters = new ArrayList<>(filters);
        }

        @Override
        public void filter(FilterArgs<Props> args) {
            for (Filter<Props> filter : filters) {
                executing(filter, () -> {
                    filter.filter(args);
                });
            }
        }

        @Override
        public SortedMap<String, Object> getParameters() {
            SortedMap<String, Object> map = new TreeMap<>();
            for (CacheableFilter<Props> filter : filters) {
                SortedMap<String, Object> subMap = filter.getParameters();
                if (subMap == null || subMap.isEmpty()) {
                    continue;
                }
                for (Map.Entry<String, Object> e : subMap.entrySet()) {
                    String key = e.getKey();
                    if (key == null || key.isEmpty()) {
                        throw new IllegalStateException(
                                "The method `getParameters` of \"" +
                                        filter.getClass().getName() +
                                        "\" cannot map with null or empty key"
                        );
                    }
                    Object value = e.getValue();
                    if (value == null) {
                        continue;
                    }
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

        @Nullable
        @Override
        public Collection<?> getAffectedSourceIds(@NotNull EntityEvent<?> e) {
            return affectedSourceIds(filters, e);
        }

        @Nullable
        @Override
        public Collection<?> getAffectedSourceIds(@NotNull AssociationEvent e) {
            return affectedSourceIds(filters, e);
        }

        @Override
        public boolean isLogicalDeletedFilter() {
            return filters.size() == 1 && filters.get(0) instanceof LogicalDeletedFilterProvider.DefaultFilter;
        }

        @Override
        public String toString() {
            return "ExportedCacheableFilter{" +
                    "filters=" + filters +
                    '}';
        }
    }

    public interface Exported {

        boolean isLogicalDeletedFilter();
    }

    private static SortedMap<String, Object> standardParameterMap(SortedMap<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return parameters;
        }
        boolean hasNullValue = false;
        for (Object o : parameters.values()) {
            if (o == null) {
                hasNullValue = true;
                break;
            }
        }
        if (!hasNullValue) {
            return parameters;
        }
        SortedMap<String, Object> withoutNullValueMap = new TreeMap<>();
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            Object value = e.getValue();
            if (value != null) {
                withoutNullValueMap.put(e.getKey(), value);
            }
        }
        return withoutNullValueMap;
    }

    public static boolean hasUserFilter(Filter<?> filter) {
        if (filter == null) {
            return false;
        }
        return !(filter instanceof Exported) || !((Exported)filter).isLogicalDeletedFilter();
    }
}