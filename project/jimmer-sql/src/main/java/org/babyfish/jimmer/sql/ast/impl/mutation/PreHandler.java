package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.lang.Lazy;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.babyfish.jimmer.sql.KeyUniqueConstraint;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.util.ConcattedIterator;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.UnloadedVersionBehavior;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.IdOnlyFetchType;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

interface PreHandler {

    void add(DraftSpi draft);

    default ShapedEntityMap<DraftSpi> insertedMap() {
        return ShapedEntityMap.empty();
    }

    default ShapedEntityMap<DraftSpi> updatedMap() {
        return ShapedEntityMap.empty();
    }

    default ShapedEntityMap<DraftSpi> mergedMap() {
        return ShapedEntityMap.empty();
    }

    @Nullable
    default Map<Object, ImmutableSpi> originalIdObjMap() {
        return null;
    }

    @Nullable
    default Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalkeyObjMap() {
        return null;
    }

    default Iterable<Batch<DraftSpi>> batches() {
        return new Iterable<Batch<DraftSpi>>() {
            @NotNull
            @Override
            public Iterator<Batch<DraftSpi>> iterator() {
                return ConcattedIterator.of(
                        insertedMap().iterator(),
                        updatedMap().iterator(),
                        mergedMap().iterator()
                );
            }
        };
    }

    Iterable<Batch<DraftSpi>> associationBatches();

    static PreHandler of(SaveContext ctx) {
        switch (ctx.options.getMode()) {
            case INSERT_ONLY:
                return new InsertPreHandler(ctx);
            case INSERT_IF_ABSENT:
                return new UpsertPreHandler(ctx, true);
            case UPDATE_ONLY:
                return new UpdatePreHandler(ctx);
            case NON_IDEMPOTENT_UPSERT:
                return new NonIdempotentUpsertHandler(ctx);
            default:
                return new UpsertPreHandler(ctx, false);
        }
    }
}

abstract class AbstractPreHandler implements PreHandler {

    private static final Object UNLOADED_COLUMN_VALUE = new Object();

    final SaveContext ctx;

    private final DraftPreProcessor<DraftSpi> processor;

    private final DraftInterceptor<Object, DraftSpi> interceptor;

    private final ImmutableProp idProp;

    final KeyMatcher keyMatcher;

    private final ImmutableProp versionProp;

    final Set<Object> validatedIds;

    final List<DraftSpi> draftsWithNothing;

    final List<DraftSpi> draftsWithId = new ArrayList<>();

    final List<DraftSpi> draftsWithKey = new ArrayList<>();

    private Map<Object, ImmutableSpi> idObjMap;

    private Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> keyObjMap;

    private Fetcher<ImmutableSpi> originalFetcher;

    private ShapedEntityMap<DraftSpi> associationMap;

    private boolean resolved;

    @SuppressWarnings("unchecked")
    AbstractPreHandler(SaveContext ctx) {
        this.ctx = ctx;
        this.processor = (DraftPreProcessor<DraftSpi>)
                ctx.options.getSqlClient().getDraftPreProcessor(ctx.path.getType());
        this.interceptor = (DraftInterceptor<Object, DraftSpi>)
                ctx.options.getSqlClient().getDraftInterceptor(ctx.path.getType());
        idProp = ctx.path.getType().getIdProp();
        keyMatcher = ctx.options.getKeyMatcher(ctx.path.getType());
        versionProp = ctx.path.getType().getVersionProp();
        if (ctx.path.getProp() != null && ctx.options.isAutoCheckingProp(ctx.path.getProp())) {
            validatedIds = new HashSet<>();
        } else {
            validatedIds = null;
        }
        if (isWildObjectAcceptable()) {
            draftsWithNothing = new ArrayList<>();
        } else {
            draftsWithNothing = null;
        }
    }

    @Override
    public Iterable<Batch<DraftSpi>> associationBatches() {
        ShapedEntityMap<DraftSpi> am = associationMap;
        if (am == null) {
            List<DraftSpi> drafts = new ArrayList<>();
            for (Batch<DraftSpi> batch : batches()) {
                drafts.addAll(batch.entities());
            }
            this.associationMap = am = createEntityMap(
                    drafts,
                    null,
                    null,
                    prop -> prop.isId() || (prop.isAssociation(TargetLevel.ENTITY) && !prop.isColumnDefinition()),
                    SaveMode.UPSERT,
                    null
            );
        }
        return am;
    }

    @Override
    public void add(DraftSpi draft) {
        Lazy<Boolean> hasNonIdValues = new Lazy<>(() -> {
            for (ImmutableProp prop : draft.__type().getProps().values()) {
                if (!prop.isId() && draft.__isLoaded(prop.getId())) {
                    return true;
                }
            }
            return false;
        });
        ImmutableProp prop = ctx.path.getProp();
        if (prop != null && prop.isRemote() && hasNonIdValues.get()) {
            ctx.throwLongRemoteAssociation();
        }
        if (draft.__isLoaded(draft.__type().getIdProp().getId())) {
            if (ctx.options.isIdOnlyAsReference(prop) &&
                    ctx.options.getUnloadedVersionBehavior(draft.__type()) == UnloadedVersionBehavior.IGNORE &&
                    !hasNonIdValues.get()
            ) {
                if (validatedIds != null) {
                    validatedIds.add(draft.__get(draft.__type().getIdProp().getId()));
                }
                return;
            }
        }
        KeyMatcher.Group group = keyMatcher.match(draft);
        callPreProcessor(draft, group);
        if (draft.__isLoaded(idProp.getId())) {
            draftsWithId.add(draft);
        } else if (group == null) {
            if (draftsWithNothing == null) {
                ctx.throwNeitherIdNorKey(draft.__type(), Collections.emptySet());
                return;
            }
            draftsWithNothing.add(draft);
        } else {
            draftsWithKey.add(draft);
        }
    }

    @Override
    public @Nullable Map<Object, ImmutableSpi> originalIdObjMap() {
        return idObjMap;
    }

    @Override
    public @Nullable Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalkeyObjMap() {
        return keyObjMap;
    }

    final Map<Object, ImmutableSpi> findOldMapByIds(QueryReason queryReason) {
        Map<Object, ImmutableSpi> idObjMap = this.idObjMap;
        if (idObjMap == null) {
            this.idObjMap = idObjMap = Rows.findMapByIds(
                    ctx,
                    queryReason,
                    originalFetcher(),
                    draftsWithId
            );
        }
        return idObjMap;
    }

    final Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> findOldMapByKeys(QueryReason queryReason) {
        Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> keyObjMap = this.keyObjMap;
        if (keyObjMap == null) {
            this.keyObjMap = keyObjMap = Rows.findMapByKeys(
                    ctx,
                    queryReason,
                    originalFetcher(),
                    draftsWithKey
            );
            if (!keyObjMap.isEmpty()) {
                Map<Object, ImmutableSpi> idObjMap = this.idObjMap;
                if (idObjMap == null) {
                    this.idObjMap = idObjMap = new HashMap<>();
                }
                PropId idPropId = ctx.path.getType().getIdProp().getId();
                for (Map<Object, ImmutableSpi> subMap : keyObjMap.values()) {
                    for (ImmutableSpi row : subMap.values()) {
                        if (row.__isLoaded(idPropId)) {
                            idObjMap.put(row.__get(idPropId), row);
                        }
                    }
                }
            }
        }
        return keyObjMap;
    }

    boolean isWildObjectAcceptable() {
        return false;
    }

    @SuppressWarnings("unchecked")
    private Fetcher<ImmutableSpi> originalFetcher() {
        Fetcher<ImmutableSpi> oldFetcher = this.originalFetcher;
        if (oldFetcher == null) {
            ImmutableType type = ctx.path.getType();
            FetcherImplementor<ImmutableSpi> fetcherImplementor =
                    new FetcherImpl<>((Class<ImmutableSpi>)ctx.path.getType().getJavaClass());
            for (ImmutableProp keyProp : keyMatcher.getAllProps()) {
                fetcherImplementor = fetcherImplementor.add(keyProp.getName(), IdOnlyFetchType.RAW);
            }
            DraftInterceptor<?, ?> interceptor = ctx.options.getSqlClient().getDraftInterceptor(type);
            if (interceptor != null) {
                Collection<? extends TypedProp<?, ?>> typedProps = interceptor.dependencies();
                if (typedProps != null) {
                    for (TypedProp<?, ?> typedProp : typedProps) {
                        fetcherImplementor = fetcherImplementor.add(typedProp.unwrap().getName(), IdOnlyFetchType.RAW);
                    }
                }
            }
            if (ctx.backReferenceFrozen) {
                fetcherImplementor = fetcherImplementor.add(ctx.backReferenceProp.getName(), IdOnlyFetchType.RAW);
            }
            this.originalFetcher = oldFetcher = fetcherImplementor;
        }
        return oldFetcher;
    }

    final QueryReason queryReason(boolean hasId, Collection<DraftSpi> drafts) {
        if (ctx.trigger != null) {
            return QueryReason.TRIGGER;
        }
        if (ctx.backReferenceFrozen) {
            return QueryReason.TARGET_NOT_TRANSFERABLE;
        }
        if (interceptor != null) {
            return QueryReason.INTERCEPTOR;
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        SaveMode saveMode = ctx.options.getMode();
        boolean clearMode = saveMode == SaveMode.INSERT_ONLY || saveMode == SaveMode.UPDATE_ONLY;
        if (!clearMode && !sqlClient.getDialect().isUpsertSupported()) {
            return QueryReason.UPSERT_NOT_SUPPORTED;
        }
        if (!hasId) {
            if (!clearMode && !ctx.options.getSqlClient().getDialect().isNoIdUpsertSupported()) {
                return QueryReason.NO_ID_UPSERT_NOT_SUPPORTED;
            }
            if (saveMode != SaveMode.UPDATE_ONLY) {
                IdGenerator idGenerator = ctx.options.getSqlClient().getIdGenerator(ctx.path.getType().getJavaClass());
                if (idGenerator == null) {
                    ctx.throwNoIdGenerator();
                }
                ImmutableProp prop = ctx.path.getProp();
                if (prop != null && ctx.options.isKeyOnlyAsReference(prop) && isKeyOnly(drafts)) {
                    return QueryReason.KEY_ONLY_AS_REFERENCE;
                }
                if (!(idGenerator instanceof IdentityIdGenerator)) {
                    return QueryReason.IDENTITY_GENERATOR_REQUIRED;
                }
            }
        }
        if (!clearMode) {
            if (saveMode != SaveMode.INSERT_IF_ABSENT &&
                    !sqlClient.getDialect().isUpsertWithOptimisticLockSupported()) {
                UserOptimisticLock<?, ?> userLock = ctx.options.getUserOptimisticLock(ctx.path.getType());
                boolean useOptimisticLock = userLock != null;
                if (!useOptimisticLock) {
                    ImmutableProp versionProp = ctx.path.getType().getVersionProp();
                    if (versionProp != null) {
                        PropId versionPropId = versionProp.getId();
                        for (DraftSpi draft : draftsWithId) {
                            if (draft.__isLoaded(versionPropId)) {
                                useOptimisticLock = true;
                                break;
                            }
                        }
                    }
                }
                if (useOptimisticLock) {
                    if (userLock != null) {
                        return QueryReason.OPTIMISTIC_LOCK;
                    }
                    if (!(this instanceof UpdatePreHandler)) {
                        for (ImmutableProp prop : ctx.path.getType().getProps().values()) {
                            if (prop.isId() || !prop.isColumnDefinition()) {
                                continue;
                            }
                            PropId propId = prop.getId();
                            for (DraftSpi draft : drafts) {
                                if (draft.__isLoaded(propId)) {
                                    return QueryReason.OPTIMISTIC_LOCK;
                                }
                            }
                        }
                    }
                    PropId versionPropId = ctx.path.getType().getVersionProp().getId();
                    for (DraftSpi draft : drafts) {
                        if (draft.__isLoaded(versionPropId)) {
                            return QueryReason.OPTIMISTIC_LOCK;
                        }
                    }
                }
            }
            if (!hasId) {
                KeyUniqueConstraint constraint = ctx
                        .path
                        .getType()
                        .getJavaClass()
                        .getAnnotation(KeyUniqueConstraint.class);
                if (constraint == null) {
                    return QueryReason.KEY_UNIQUE_CONSTRAINT_REQUIRED;
                }
                if (!sqlClient.isUpsertWithUniqueConstraintSupported(ctx.path.getType())) {
                    return QueryReason.NO_MORE_UNIQUE_CONSTRAINTS_REQUIRED;
                }
                if (!constraint.isNullNotDistinct() ||
                        !sqlClient.getDialect().isUpsertWithNullableKeySupported()) {
                    for (Set<ImmutableProp> keyProps : keyMatcher.toMap().values()) {
                        List<PropertyGetter> nullableGetters = new ArrayList<>();
                        for (PropertyGetter getter : Shape.fullOf(sqlClient, ctx.path.getType().getJavaClass()).getGetters()) {
                            if (getter.prop().isNullable() && keyProps.contains(getter.prop())) {
                                nullableGetters.add(getter);
                            }
                        }
                        if (!nullableGetters.isEmpty()) {
                            for (DraftSpi draft : drafts) {
                                for (PropertyGetter nullableGetter : nullableGetters) {
                                    if (nullableGetter.get(draft) == null) {
                                        return QueryReason.NULL_NOT_DISTINCT_REQUIRED;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return QueryReason.NONE;
    }

    private void callPreProcessor(DraftSpi draft, KeyMatcher.Group group) {
        DraftPreProcessor<DraftSpi> processor = this.processor;
        if (processor == null) {
            return;
        }
        if (processor.ignoreIdOnly() &&
                ctx.options.isIdOnlyAsReference(ctx.path.getProp()) &&
                ImmutableObjects.isIdOnly(draft)) {
            return;
        }
        if (group != null &&
                ctx.options.isKeyOnlyAsReference(ctx.path.getProp()) &&
                processor.ignoreKeyOnly(group) &&
                isKeyOnly(draft, group.getProps())) {
            return;
        }
        processor.beforeSave(draft);
    }

    final void callInterceptor(List<DraftInterceptor.Item<Object, DraftSpi>> items) {
        if (items.isEmpty()) {
            return;
        }
        for (DraftInterceptor.Item<Object, DraftSpi> item : items) {
            if (item.getState().isIdOnly() && ctx.options.isIdOnlyAsReference(ctx.path.getProp())) {
                continue;
            }
            if (item.getState().isKeyOnly() && ctx.options.isKeyOnlyAsReference(ctx.path.getProp())) {
                continue;
            }
            if (item.getOriginal() == null && ctx.options.getMode() != SaveMode.UPDATE_ONLY) {
                DraftSpi draft = item.getDraft();
                assignId(draft);
                assignVersion(draft);
                assignLocalDeletedInfo(draft);
                assignDefaultValues(draft);
            }
        }
        DraftInterceptor<Object, DraftSpi> interceptor = this.interceptor;
        if (interceptor == null) {
            return;
        }
        Map<DraftPropKey, Object> idKeyColumnValueMap = new LinkedHashMap<>();
        Iterator<DraftInterceptor.Item<Object, DraftSpi>> itr = items.iterator();
        while (itr.hasNext()) {
            DraftInterceptor.Item<Object, DraftSpi> item = itr.next();
            DraftSpi draft = item.getDraft();
            DraftInterceptor.Item.State state = item.getState();
            if (state.isIdOnly() &&
                    interceptor.ignoreIdOnly() &&
                    ctx.options.isIdOnlyAsReference(ctx.path.getProp())
            ) {
                itr.remove();
                continue;
            }
            boolean hasId = collectColumnValue(draft, idProp, idKeyColumnValueMap);
            KeyMatcher.Group group = state.getKeyGroup();
            if (state.isKeyOnly()) {
                assert group != null;
                if (interceptor.ignoreKeyOnly(group) && ctx.options.isKeyOnlyAsReference(ctx.path.getProp())) {
                    itr.remove();
                    continue;
                }
            }
            if (!hasId && group != null) {
                for (ImmutableProp keyProp : group.getProps()) {
                    collectColumnValue(draft, keyProp, idKeyColumnValueMap);
                }
            }
        }
        if (items.isEmpty()) {
            return;
        }
        interceptor.beforeSaveAll(items);
        for (Map.Entry<DraftPropKey, Object> e : idKeyColumnValueMap.entrySet()) {
            DraftPropKey key = e.getKey();
            ImmutableProp prop = key.prop;
            Object value = columnValue(key.draft, prop);
            if (!Objects.equals(e.getValue(), value)) {
                ctx.throwIllegalInterceptorBehavior(prop);
            }
        }
    }

    private void assignId(DraftSpi draft) {
        PropId idPropId = idProp.getId();
        if (draft.__isLoaded(idPropId)) {
            return;
        }
        Object id = ctx.allocateId();
        if (id != null) {
            draft.__set(idPropId, id);
        }
    }

    private void assignVersion(DraftSpi draft) {
        ImmutableProp versionProp = this.versionProp;
        if (versionProp == null) {
            return;
        }
        PropId versionPropId = versionProp.getId();
        if (!draft.__isLoaded(versionPropId)) {
            draft.__set(versionPropId, 0);
        }
    }

    private void assignLocalDeletedInfo(DraftSpi draft) {
        LogicalDeletedInfo logicalDeletedInfo = ctx.path.getType().getLogicalDeletedInfo();
        if (logicalDeletedInfo == null) {
            return;
        }
        Object value = logicalDeletedInfo.allocateInitializedValue();
        draft.__set(logicalDeletedInfo.getProp().getId(), value);
    }

    // Notes: This method can only be overridden by InsertPreHandler
    // Otherwise, it is bug!
    void assignDefaultValues(DraftSpi draft) {}

    final void resolve() {
        if (!resolved) {
            validateAloneIds();
            onResolve();
            resolved = true;
        }
    }

    abstract void onResolve();

    final ShapedEntityMap<DraftSpi> createEntityMap(
            Iterable<DraftSpi> i1,
            Iterable<DraftSpi> i2,
            Iterable<DraftSpi> i3,
            SaveMode mode,
            @Nullable SaveMode originalMode
    ) {
        return createEntityMap(
                i1,
                i2,
                i3,
                ImmutableProp::isColumnDefinition,
                mode,
                originalMode
        );
    }

    final ShapedEntityMap<DraftSpi> createEntityMap(
            Iterable<DraftSpi> i1,
            Iterable<DraftSpi> i2,
            Iterable<DraftSpi> i3,
            Predicate<ImmutableProp> propFilter,
            SaveMode mode,
            @Nullable SaveMode originalMode
    ) {

        ShapedEntityMap<DraftSpi> entityMap =
                new ShapedEntityMap<>(ctx.options.getSqlClient(), keyMatcher, propFilter, mode, originalMode);
        if (i1 != null) {
            for (DraftSpi draft : i1) {
                entityMap.add(draft);
            }
        }
        if (i2 != null) {
            for (DraftSpi draft : i2) {
                entityMap.add(draft);
            }
        }
        if (i3 != null) {
            for (DraftSpi draft : i3) {
                entityMap.add(draft);
            }
        }
        return entityMap;
    }

    @SuppressWarnings("unchecked")
    private void validateAloneIds() {
        Collection<Object> ids = this.validatedIds;
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ImmutableProp prop = ctx.path.getProp();
        if (prop.isRemote()) {
            PropId targetIdPropId = prop.getTargetType().getIdProp().getId();
            List<ImmutableSpi> targets;
            try {
                targets = ctx
                        .options
                        .getSqlClient()
                        .getMicroServiceExchange()
                        .findByIds(
                                prop.getTargetType().getMicroServiceName(),
                                ids,
                                new FetcherImpl<>((Class<ImmutableSpi>) (prop.getTargetType().getJavaClass()))
                        );
            } catch (Exception ex) {
                ctx.throwFailedRemoteValidation();
                return;
            }
            if (targets.size() < ids.size()) {
                for (ImmutableSpi target : targets) {
                    ids.remove(target.__get(targetIdPropId));
                }
                throw ctx.createIllegalTargetId(ids);
            }
        } else {
            MutableRootQueryImpl<Table<Object>> q = new MutableRootQueryImpl<>(
                    ctx.options.getSqlClient(),
                    ctx.path.getType(),
                    ExecutionPurpose.MUTATE,
                    FilterLevel.IGNORE_ALL
            );
            Table<?> table = (Table<?>) q.getTableLikeImplementor();
            q.where(table.getId().in(ids));
            List<Object> actualTargetIds = q.select(table.getId()).execute(ctx.con);
            if (actualTargetIds.size() < ids.size()) {
                actualTargetIds.forEach(ids::remove);
                throw ctx.createIllegalTargetId(ids);
            }
        }
    }

    private boolean isKeyOnly(Collection<DraftSpi> drafts) {
        for (DraftSpi draft : drafts) {
            if (!isKeyOnly(draft, keyMatcher.matchedKeyProps(draft))) {
                return false;
            }
        }
        return true;
    }

    private boolean isKeyOnly(DraftSpi draft, Set<ImmutableProp> keyProps) {
        boolean hasKey = false;
        for (ImmutableProp prop : ctx.path.getType().getProps().values()) {
            if (!prop.isColumnDefinition()) {
                continue;
            }
            boolean isLoaded = draft.__isLoaded(prop.getId());
            if (isLoaded && prop.isReference(TargetLevel.PERSISTENT)) {
                Object value = draft.__get(prop.getId());
                if (value != null) {
                    ImmutableSpi target = (ImmutableSpi) value;
                    if (!target.__isLoaded(target.__type().getIdProp().getId())) {
                        isLoaded = false;
                    }
                }
            }
            if (keyProps.contains(prop)) {
                if (isLoaded) {
                    hasKey = true;
                } else {
                    return false;
                }
            } else {
                if (isLoaded) {
                    return false;
                }
            }
        }
        return hasKey;
    }

    private static Object columnValue(DraftSpi draft, ImmutableProp prop) {
        PropId propId = prop.getId();
        if (!draft.__isLoaded(propId)) {
            return UNLOADED_COLUMN_VALUE;
        }
        Object value = draft.__get(propId);
        if (value == null || !prop.isReference(TargetLevel.ENTITY)) {
            return value;
        }
        PropId targetIdPropId = prop.getTargetType().getIdProp().getId();
        return ((ImmutableSpi) value).__get(targetIdPropId);
    }

    private static boolean collectColumnValue(
            DraftSpi draft,
            ImmutableProp prop,
            Map<DraftPropKey, Object> valueMap
    ) {
        PropId propId = prop.getId();
        if (!draft.__isLoaded(propId)) {
            return false;
        }
        Object value = draft.__get(propId);
        if (value != null && prop.isReference(TargetLevel.ENTITY)) {
            PropId targetIdPropId = prop.getTargetType().getIdProp().getId();
            value = ((ImmutableSpi) value).__get(targetIdPropId);
        }
        valueMap.put(new DraftPropKey(draft, prop), value);
        return true;
    }

    protected DraftInterceptor.Item<Object, DraftSpi> newItem(DraftSpi draft, @Nullable ImmutableSpi original) {
        boolean idOnly = ImmutableObjects.isIdOnly(draft);
        KeyMatcher.Group group = keyMatcher.match(draft);
        boolean keyOnly = group != null && isKeyOnly(draft, group.getProps());
        return new DraftInterceptor.Item<>(draft, original, new DraftInterceptor.Item.State(group, idOnly, keyOnly));
    }

    private static class DraftPropKey {

        final DraftSpi draft;

        final ImmutableProp prop;

        private DraftPropKey(DraftSpi draft, ImmutableProp prop) {
            this.draft = draft;
            this.prop = prop;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(draft);
            result = 31 * result + prop.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DraftPropKey that = (DraftPropKey) o;
            if (draft != that.draft) {
                return false;
            }
            return prop.equals(that.prop);
        }
    }
}

class InsertPreHandler extends AbstractPreHandler {

    private ShapedEntityMap<DraftSpi> insertedMap;

    InsertPreHandler(SaveContext ctx) {
        super(ctx);
    }

    @Override
    public ShapedEntityMap<DraftSpi> insertedMap() {
        resolve();
        return this.insertedMap;
    }

    @Override
    void onResolve() {
        if (!draftsWithKey.isEmpty()) {
            IdGenerator idGenerator = ctx.options.getSqlClient().getIdGenerator(ctx.path.getType().getJavaClass());
            if (idGenerator instanceof UserIdGenerator<?>) {
                PropId idPropId = ctx.path.getType().getIdProp().getId();
                for (DraftSpi draft : draftsWithKey) {
                    Object id = ctx.allocateId();
                    if (id != null) {
                        draft.__set(idPropId, id);
                    }
                }
            }
        }
        List<DraftInterceptor.Item<Object, DraftSpi>> items = new ArrayList<>(
                draftsWithNothing.size() +
                        draftsWithId.size() +
                        draftsWithKey.size()
        );
        for (DraftSpi draft : draftsWithNothing) {
            items.add(newItem(draft, null));
        }
        for (DraftSpi draft : draftsWithId) {
            items.add(newItem(draft, null));
        }
        for (DraftSpi draft : draftsWithKey) {
            items.add(newItem(draft, null));
        }
        callInterceptor(items);

        this.insertedMap = createEntityMap(
                draftsWithNothing,
                draftsWithId,
                draftsWithKey,
                SaveMode.INSERT_ONLY,
                null
        );
    }

    @Override
    boolean isWildObjectAcceptable() {
        return true;
    }

    @Override
    void assignDefaultValues(DraftSpi draft) {
        for (ImmutableProp prop : draft.__type().getProps().values()) {
            PropId propId = prop.getId();
            if (draft.__isLoaded(propId)) {
                continue;
            }
            Ref<Object> ref = prop.getDefaultValueRef();
            if (ref == null) {
                continue;
            }
            Object v = ref.getValue();
            Object evaluatedValue = v instanceof Supplier<?> ? ((Supplier<?>) v).get() : v;
            draft.__set(propId, evaluatedValue);
        }
    }
}

class UpdatePreHandler extends AbstractPreHandler {

    UpdatePreHandler(SaveContext ctx) {
        super(ctx);
    }

    private ShapedEntityMap<DraftSpi> updatedMap;

    @Override
    public ShapedEntityMap<DraftSpi> updatedMap() {
        resolve();
        return updatedMap;
    }

    @Override
    void onResolve() {

        List<DraftInterceptor.Item<Object, DraftSpi>> items = new ArrayList<>(
                draftsWithId.size() + draftsWithKey.size()
        );

        PropId idPropId = ctx.path.getType().getIdProp().getId();

        if (!draftsWithId.isEmpty()) {
            QueryReason queryReason = queryReason(true, draftsWithId);
            if (queryReason != QueryReason.NONE) {
                Map<Object, ImmutableSpi> idMap = findOldMapByIds(queryReason);
                Iterator<DraftSpi> itr = draftsWithId.iterator();
                while (itr.hasNext()) {
                    DraftSpi draft = itr.next();
                    Object id = draft.__get(idPropId);
                    ImmutableSpi original = idMap.get(id);
                    if (original != null) {
                        items.add(newItem(draft, original));
                    } else {
                        itr.remove();
                    }
                }
            }
        }

        if (!draftsWithKey.isEmpty()) {
            QueryReason queryReason = queryReason(false, draftsWithKey);
            if (queryReason != QueryReason.NONE) {
                Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> keyMap = findOldMapByKeys(queryReason);
                Iterator<DraftSpi> itr = draftsWithKey.iterator();
                while (itr.hasNext()) {
                    DraftSpi draft = itr.next();
                    KeyMatcher.Group group = keyMatcher.match(draft);
                    assert group != null;
                    Object key = Keys.keyOf(draft, group.getProps());
                    Map<Object, ImmutableSpi> subMap = keyMap.getOrDefault(group, Collections.emptyMap());
                    ImmutableSpi original = subMap.get(key);
                    if (original != null) {
                        items.add(newItem(draft, original));
                        draft.__set(idPropId, original.__get(idPropId));
                    } else {
                        itr.remove();
                    }
                }
            }
        }
        callInterceptor(items);

        this.updatedMap = createEntityMap(
                null,
                draftsWithId,
                draftsWithKey,
                SaveMode.UPDATE_ONLY,
                null
        );
    }
}

class UpsertPreHandler extends AbstractPreHandler {

    private final boolean ignoreUpdate;

    private ShapedEntityMap<DraftSpi> insertedMap;

    private ShapedEntityMap<DraftSpi> updatedMap;

    private ShapedEntityMap<DraftSpi> mergedMap;

    UpsertPreHandler(SaveContext ctx, boolean ignoreUpdate) {
        super(ctx);
        this.ignoreUpdate = ignoreUpdate;
    }

    @Override
    public ShapedEntityMap<DraftSpi> insertedMap() {
        resolve();
        return insertedMap;
    }

    @Override
    public ShapedEntityMap<DraftSpi> updatedMap() {
        resolve();
        return updatedMap;
    }

    @Override
    public ShapedEntityMap<DraftSpi> mergedMap() {
        resolve();
        return mergedMap;
    }

    @Override
    void onResolve() {

        PropId idPropId = ctx.path.getType().getIdProp().getId();
        List<DraftSpi> insertedList = null;
        List<DraftSpi> updatedList = null;
        List<DraftSpi> updatedWithoutKeyList = null;

        List<DraftInterceptor.Item<Object, DraftSpi>> items = new ArrayList<>(
                (draftsWithNothing != null ? draftsWithNothing.size() : 0)+
                        draftsWithId.size() +
                        draftsWithKey.size()
        );

        if (draftsWithNothing != null && !draftsWithNothing.isEmpty()) {
            for (DraftSpi draft : draftsWithNothing) {
                items.add(newItem(draft, null));
            }
        }

        if (!draftsWithId.isEmpty()) {
            QueryReason queryReason = queryReason(true, draftsWithId);
            if (queryReason != QueryReason.NONE) {
                insertedList = new ArrayList<>();
                updatedList = new ArrayList<>();
                Map<Object, ImmutableSpi> idMap = findOldMapByIds(queryReason);
                Iterator<DraftSpi> itr = draftsWithId.iterator();
                while (itr.hasNext()) {
                    DraftSpi draft = itr.next();
                    ImmutableSpi original = idMap.get(draft.__get(idPropId));
                    if (original == null) {
                        insertedList.add(draft);
                        itr.remove();
                        items.add(newItem(draft, null));
                    } else if (!ignoreUpdate) {
                        updatedList.add(draft);
                        items.add(newItem(draft, original));
                    }
                }
            } else {
                updatedList = new ArrayList<>(draftsWithId);
            }
        }

        if (!draftsWithKey.isEmpty()) {
            QueryReason queryReason = queryReason(false, draftsWithKey);
            if (queryReason != QueryReason.NONE) {
                if (insertedList == null) {
                    insertedList = new ArrayList<>();
                }
                updatedWithoutKeyList = new ArrayList<>();
                Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> keyMap = findOldMapByKeys(queryReason);
                Iterator<DraftSpi> itr = draftsWithKey.iterator();
                while (itr.hasNext()) {
                    DraftSpi draft = itr.next();
                    KeyMatcher.Group group = ctx.options.getKeyMatcher(ctx.path.getType()).match(draft);
                    assert group != null;
                    Object key = Keys.keyOf(draft, group.getProps());
                    Map<Object, ImmutableSpi> subMap = keyMap.getOrDefault(group, Collections.emptyMap());
                    ImmutableSpi original = subMap.get(key);
                    if (original == null) {
                        insertedList.add(draft);
                        itr.remove();
                        items.add(newItem(draft, null));
                    } else {
                        if (!ignoreUpdate) {
                            updatedWithoutKeyList.add(draft);
                            items.add(newItem(draft, original));
                        }
                        if (!ignoreUpdate || ctx.isIdRetrievingRequired()) {
                            draft.__set(idPropId, original.__get(idPropId));
                        }
                    }
                }
            }
        }
        callInterceptor(items);

        this.insertedMap = createEntityMap(null, insertedList, draftsWithNothing, SaveMode.INSERT_ONLY, SaveMode.UPSERT);
        if (insertedList == null) {
            this.updatedMap = ShapedEntityMap.empty();
            this.mergedMap = createEntityMap(
                    null,
                    draftsWithId,
                    draftsWithKey,
                    ignoreUpdate ? SaveMode.INSERT_IF_ABSENT : SaveMode.UPSERT,
                    SaveMode.UPSERT
            );
        } else if (ignoreUpdate) {
            this.updatedMap = ShapedEntityMap.empty();
            this.mergedMap = ShapedEntityMap.empty();
        } else {
            this.updatedMap = createEntityMap(null, updatedList, null, SaveMode.UPDATE_ONLY, SaveMode.UPSERT);
            if (updatedWithoutKeyList != null && !updatedWithoutKeyList.isEmpty()) {
                ShapedEntityMap<DraftSpi> updatedMap = this.updatedMap;
                for (DraftSpi draft : updatedWithoutKeyList) {
                    updatedMap.add(draft, true);
                }
            }
            // key如果在数据库存在的情况下 updatedMap不为空 如果不做判断会导致多一条merge sql
            if (updatedMap != null) {
                this.mergedMap = ShapedEntityMap.empty();
            } else {
                this.mergedMap = createEntityMap(
                        null,
                        draftsWithId,
                        draftsWithKey,
                        SaveMode.UPSERT,
                        SaveMode.UPSERT
                );
            }
        }
    }
}

class NonIdempotentUpsertHandler extends UpsertPreHandler {

    NonIdempotentUpsertHandler(SaveContext ctx) {
        super(ctx, false);
    }

    @Override
    boolean isWildObjectAcceptable() {
        return true;
    }
}

