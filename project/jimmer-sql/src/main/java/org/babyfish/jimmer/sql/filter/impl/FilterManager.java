package org.babyfish.jimmer.sql.filter.impl;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.cache.impl.PropCacheInvalidators;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.PropsFor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.cache.CachesImpl;
import org.babyfish.jimmer.sql.cache.LocatedCache;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.*;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class FilterManager implements Filters {

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
            LogicalDeletedFilterProvider provider,
            List<Filter<?>> filters,
            Collection<Filter<?>> disabledFilters
    ) {
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
            LogicalDeletedFilterProvider provider,
            Set<Filter<?>> filters,
            Set<Filter<?>> disabledFilters,
            Map<String, List<Filter<Props>>> filterMap,
            Map<String, List<CacheableFilter<Props>>> allCacheableFilterMap
    ) {
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
            return Ref.of(((CacheableFilter<Props>) filter).getParameters());
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
                newProvider,
                allFilters,
                disabledFilters,
                filterMap,
                allCacheableFilterMap
        );
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

    public boolean contains(ImmutableType type) {
        for (ImmutableType t : type.getAllTypes()) {
            if (filterMap.containsKey(t.toString())) {
                // No matter enabled or disabled
                return true;
            }
        }
        return false;
    }

    public boolean isNullableRequired(ImmutableProp prop) {
        if (!prop.isReference(TargetLevel.ENTITY)) {
            return false;
        }
        if (prop.getDeclaringType() == prop.getTargetType()) {
            return false;
        }
        Set<Filter<Props>> declaredFilters = new HashSet<>();
        for (ImmutableType t : prop.getDeclaringType().getAllTypes()) {
            List<Filter<Props>> filters = filterMap.get(t.toString());
            if (filters == null) {
                continue;
            }
            for (Filter<Props> filter : filters) {
                if (!(filter instanceof AssociationIntegrityAssuranceFilter<?>)) {
                    return true;
                }
            }
            declaredFilters.addAll(filters);
        }
        for (ImmutableType t : prop.getTargetType().getAllTypes()) {
            List<Filter<Props>> filters = filterMap.get(t.toString());
            if (filters != null && !declaredFilters.containsAll(filters)) {
                return true;
            }
        }
        return false;
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
        // Cannot optimize when `.size() == 1`
        for (Filter<?> filter : filters) {
            if (!(filter instanceof CacheableFilter<?>)) {
                return new CompositeFilter(filters);
            }
        }
        return new CompositeCacheableFilter(type, (Collection<CacheableFilter<Props>>)(Collection<?>)filters);
    }

    private List<CacheableFilter<Props>> createAllCacheable(ImmutableType type) {
        List<CacheableFilter<Props>> filters = new ArrayList<>();
        for (ImmutableType t : type.getAllTypes()) {
            List<CacheableFilter<Props>> list = allCacheableFilterMap.get(t.toString());
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
        for (Map.Entry<ImmutableType, LocatedCache<?, ?>> entry : caches.getObjectCacheMap().entrySet()) {
            ImmutableType type = entry.getKey();
            List<CacheableFilter<Props>> filters = allCacheableCache.get(type);
            if (!filters.isEmpty() && PropCacheInvalidators.isGetAffectedSourceIdsOverridden(filters, EntityEvent.class)) {
                sqlClient.getTriggers().addEntityListener(e -> {
                    EvictContext.execute(() -> {
                        handleOtherChange(type, filters, e);
                    });
                });
            }
            if (!filters.isEmpty() && PropCacheInvalidators.isGetAffectedSourceIdsOverridden(filters, AssociationEvent.class)) {
                sqlClient.getTriggers().addAssociationListener(e -> {
                    EvictContext.execute(() -> {
                        handleOtherChange(type, filters, e);
                    });
                });
            }
        }
        for (Map.Entry<ImmutableProp, LocatedCache<?, ?>> entry : caches.getPropCacheMap().entrySet()) {
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
            fireAssociationEvent(prop, e);
            return;
        }
        if (prop.isReferenceList(TargetLevel.PERSISTENT)) {
            ImmutableProp mappedBy = prop.getMappedBy();
            if (mappedBy != null && mappedBy.isColumnDefinition()) {
                if (e.getUnchangedRef(mappedBy) == null) {
                    return;
                }
            }
        }
        boolean affected = false;
        for (CacheableFilter<Props> filter : filters) {
            if (filter.isAffectedBy(e)) {
                affected = true;
                break;
            }
        }
        if (affected) {
            fireAssociationEvent(prop, e);
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
                if (ctx == null || ctx.add(type, sourceId)) {
                    triggers.fireEntityEvict(type, sourceId, e.getConnection());
                }
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
            Object detachedTargetId = e.getDetachedTargetId();
            Object attachedTargetId = e.getAttachedTargetId();
            ImmutableProp backProp = e.getImmutableProp().getOpposite();
            if (detachedTargetId != null) {
                ctx.add(e.getImmutableProp().getTargetType(), detachedTargetId);
                if (backProp != null) {
                    ctx.add(backProp, detachedTargetId);
                }
            }
            if (attachedTargetId != null) {
                ctx.add(e.getImmutableProp().getTargetType(), attachedTargetId);
                if (backProp != null) {
                    ctx.add(backProp, attachedTargetId);
                }
            }
        }
        Collection<?> sourceIds = affectedSourceIds(filters, e);
        if (sourceIds != null) {
            Triggers triggers = sqlClient.getTriggers();
            for (Object sourceId : sourceIds) {
                if (ctx == null || ctx.add(type, sourceId)) {
                    triggers.fireEntityEvict(type, sourceId, e.getConnection());
                }
            }
        }
    }

    private void fireAssociationEvent(ImmutableProp prop, EntityEvent<?> e) {
        EvictContext ctx = EvictContext.get();
        Triggers triggers = sqlClient.getTriggers();
        ImmutableProp mappedBy = prop.getMappedBy();
        if (mappedBy != null && mappedBy.isColumnDefinition()) {
            if (e.isEvict()) {
                ImmutableType targetType = prop.getTargetType();
                ImmutableType sourceType = mappedBy.getTargetType();
                Object parentId = Queries
                        .createQuery(sqlClient, targetType, ExecutionPurpose.EVICT, true, (q, target) -> {
                            Expression<Object> targetIdExpr = target.get(targetType.getIdProp().getName());
                            Expression<Object> sourceIdExpr = target.join(mappedBy.getName()).get(sourceType.getIdProp().getName());
                            q.where(targetIdExpr.eq(e.getId()));
                            return q.select(sourceIdExpr);
                        })
                        .fetchOneOrNull();
                if (parentId != null && (ctx == null || ctx.add(prop, parentId))) {
                    triggers.fireAssociationEvict(prop, parentId, e.getConnection());
                }
            } else {
                Ref<Object> ref = e.getUnchangedRef(mappedBy);
                if (ref != null) {
                    ImmutableSpi source = (ImmutableSpi) ref.getValue();
                    if (source != null) {
                        ImmutableType sourceType = source.__type();
                        Object sourceId = source.__get(sourceType.getIdProp().getId());
                        if (ctx == null || ctx.add(prop, sourceId)) {
                            triggers.fireAssociationEvict(prop, sourceId, e.getConnection());
                        }
                    }
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
                        .createQuery(sqlClient, sourceType, ExecutionPurpose.EVICT, true, (q, source) -> {
                            Expression<Object> sourceIdExpr = source.get(sourceType.getIdProp().getName());
                            Expression<Object> targetIdExpr = source.join(prop.getName()).get(targetIdPropName);
                            q.where(targetIdExpr.eq(e.getId()));
                            return q.select(sourceIdExpr);
                        })
                        .distinct()
                        .execute();
                for (Object sourceId : sourceIds) {
                    if (ctx == null || ctx.add(prop, sourceId)) {
                        triggers.fireAssociationEvict(prop, sourceId, e.getConnection());
                    }
                }
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

    private static class CompositeFilter implements Filter<Props> {

        private final List<Filter<Props>> filters;

        private CompositeFilter(Collection<Filter<Props>> filters) {
            this.filters = new ArrayList<>(filters);
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

        private CompositeCacheableFilter(ImmutableType type, Collection<CacheableFilter<Props>> filters) {
            this.type = type;
            this.filters = new ArrayList<>(filters);
        }

        @Override
        public void filter(FilterArgs<Props> args) {
            for (Filter<Props> filter : filters) {
                filter.filter(args);
            }
        }

        @Override
        public SortedMap<String, Object> getParameters() {
            if (filters.size() == 1) {
                SortedMap<String, Object> map = filters.get(0).getParameters();
                return map != null ? map : Collections.emptySortedMap();
            }
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
                        throw new IllegalStateException(
                                "The method `getParameters` of \"" +
                                        filter.getClass().getName() +
                                        "\" cannot map with null value"
                        );
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
        public String toString() {
            return "CompositeCacheableFilter{" +
                    "filters=" + filters +
                    '}';
        }
    }

    private static class EvictContext {

        private static final ThreadLocal<EvictContext> LOCAL = new ThreadLocal<>();

        private final Set<EvictItem> items = new HashSet<>();

        private final Set<ImmutableProp> disabledAssociations = new HashSet<>();

        public static void execute(Runnable block) {
            EvictContext ctx = LOCAL.get();
            if (ctx != null) {
                block.run();
            } else {
                ctx = new EvictContext();
                LOCAL.set(ctx);
                try {
                    block.run();
                } finally {
                    LOCAL.remove();
                }
            }
        }

        @Nullable
        public static EvictContext get() {
            return LOCAL.get();
        }

        public boolean add(ImmutableType type, Object id) {
            return items.add(new EvictItem(type, id));
        }

        public boolean add(ImmutableProp prop, Object id) {
            if (disabledAssociations.contains(prop)) {
                return false;
            }
            return items.add(new EvictItem(prop, id));
        }

        public void disable(ImmutableProp prop) {
            this.disabledAssociations.add(prop);
            ImmutableProp opposite = prop.getOpposite();
            if (opposite != null) {
                this.disabledAssociations.add(opposite);
            }
        }
    }

    private static class EvictItem {

        final Object sourceId;
        final Object meta;

        EvictItem(ImmutableType type, Object sourceId) {
            this.meta = type;
            this.sourceId = sourceId;
        }

        EvictItem(ImmutableProp prop, Object sourceId) {
            this.meta = prop;
            this.sourceId = sourceId;
        }

        @Override
        public int hashCode() {
            int result = sourceId.hashCode();
            result = 31 * result + meta.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EvictItem evictItem = (EvictItem) o;

            if (!sourceId.equals(evictItem.sourceId)) return false;
            return meta.equals(evictItem.meta);
        }

        @Override
        public String toString() {
            return "EvictItem{" +
                    "sourceId=" + sourceId +
                    ", meta=" + meta +
                    '}';
        }
    }
}