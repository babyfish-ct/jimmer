package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.lang.Lazy;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.KeyUniqueConstraint;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.impl.util.ConcattedIterator;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.mutation.LockMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
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
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

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
    default Map<Object, ImmutableSpi> originalkeyObjMap() {
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
            case UPDATE_ONLY:
                return new UpdatePreHandler(ctx);
            default:
                return new UpsertPreHandler(ctx);
        }
    }
}

abstract class AbstractPreHandler implements PreHandler {

    final SaveContext ctx;

    private final DraftPreProcessor<DraftSpi> processor;

    private final DraftInterceptor<Object, DraftSpi> interceptor;

    private final ImmutableProp idProp;

    final Set<ImmutableProp> keyProps;

    private final ImmutableProp versionProp;

    final Set<Object> validatedIds;

    final List<DraftSpi> draftsWithId = new ArrayList<>();

    final List<DraftSpi> draftsWithKey = new ArrayList<>();

    private Map<Object, ImmutableSpi> idObjMap;

    private Map<Object, ImmutableSpi> keyObjMap;

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
        keyProps = ctx.path.getType().getKeyProps();
        versionProp = ctx.path.getType().getVersionProp();
        if (ctx.path.getProp() != null && ctx.options.isAutoCheckingProp(ctx.path.getProp())) {
            validatedIds = new HashSet<>();
        } else {
            validatedIds = null;
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
                    prop -> prop.isId() || (prop.isAssociation(TargetLevel.ENTITY) && !prop.isColumnDefinition()),
                    SaveMode.UPSERT
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
        if (ctx.path.getProp() != null && ctx.path.getProp().isRemote() && hasNonIdValues.get()) {
            ctx.throwLongRemoteAssociation();
        }
        if (draft.__isLoaded(draft.__type().getIdProp().getId())) {
            if (!hasNonIdValues.get()) {
                if (validatedIds != null) {
                    validatedIds.add(draft.__get(draft.__type().getIdProp().getId()));
                }
                return;
            }
        } else {
            Set<ImmutableProp> keyProps = ctx.options.getKeyProps(draft.__type());
            for (ImmutableProp keyProp : keyProps) {
                if (!draft.__isLoaded(keyProp.getId())) {
                    ctx.throwNeitherIdNorKey(draft, keyProp);
                }
            }
        }
        if (processor != null) {
            processor.beforeSave(draft);
        }
        if (draft.__isLoaded(idProp.getId())) {
            draftsWithId.add(draft);
        } else if (keyProps.isEmpty()) {
            if (ctx.options.getMode() != SaveMode.INSERT_ONLY) {
                throw new SaveException.NoKeyProps(
                        ctx.path,
                        "Cannot save \"" +
                                ctx.path.getType() +
                                "\" that have no properties decorated by \"@" +
                                Key.class.getName() +
                                "\""
                );
            }
            draftsWithKey.add(draft);
        } else {
            for (ImmutableProp keyProp : keyProps) {
                if (!draft.__isLoaded(keyProp.getId())) {
                    throw new SaveException.NoKeyProp(
                            ctx.path,
                            "Cannot save \"" +
                                    ctx.path.getType() +
                                    "\" with the unloaded key property \"" +
                                    keyProp +
                                    "\""
                    );
                }
            }
            draftsWithKey.add(draft);
        }
    }

    @Override
    public @Nullable Map<Object, ImmutableSpi> originalIdObjMap() {
        return idObjMap;
    }

    @Override
    public @Nullable Map<Object, ImmutableSpi> originalkeyObjMap() {
        return keyObjMap;
    }

    Map<Object, ImmutableSpi> findOldMapByIds(QueryReason queryReason) {
        Map<Object, ImmutableSpi> idObjMap = this.idObjMap;
        if (idObjMap == null) {
            this.idObjMap = idObjMap = findOldMapByIdsImpl(queryReason);
        }
        return idObjMap;
    }

    private Map<Object, ImmutableSpi> findOldMapByIdsImpl(QueryReason queryReason) {
        Set<Object> ids = new LinkedHashSet<>(draftsWithId.size());
        boolean isRoot = ctx.path.getParent() == null;
        for (DraftSpi draft : draftsWithId) {
            for (ImmutableProp prop : draft.__type().getProps().values()) {
                if (isRoot || (!prop.isId() && draft.__isLoaded(prop.getId()))) {
                    ids.add(draft.__get(idProp.getId()));
                    break;
                }
            }
        }
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        List<ImmutableSpi> entities = findOldList(queryReason, (q, t) -> {
            q.where(t.get(idProp).in(ids));
        });
        if (entities.isEmpty()) {
            return new HashMap<>();
        }
        Map<Object, ImmutableSpi> map = new LinkedHashMap<>((entities.size() * 4 + 2) / 3);
        for (ImmutableSpi entity : entities) {
            map.put(entity.__get(idProp.getId()), entity);
        }
        return map;
    }

    Map<Object, ImmutableSpi> findOldMapByKeys(QueryReason queryReason) {
        Map<Object, ImmutableSpi> keyObjMap = this.keyObjMap;
        if (keyObjMap == null) {
            this.keyObjMap = keyObjMap = findOldMapByKeyImpl(queryReason);
            if (!keyObjMap.isEmpty()) {
                Map<Object, ImmutableSpi> idObjMap = this.idObjMap;
                if (idObjMap == null) {
                    this.idObjMap = idObjMap = new HashMap<>();
                }
                PropId idPropId = ctx.path.getType().getIdProp().getId();
                for (ImmutableSpi row : keyObjMap.values()) {
                    if (row.__isLoaded(idPropId)) {
                        idObjMap.put(row.__get(idPropId), row);
                    }
                }
            }
        }
        return keyObjMap;
    }

    private Map<Object, ImmutableSpi> findOldMapByKeyImpl(QueryReason queryReason) {
        Collection<ImmutableProp> keyProps = this.keyProps;
        Set<Object> keys = new LinkedHashSet<>(draftsWithKey.size());
        for (DraftSpi draft : draftsWithKey) {
            keys.add(Keys.keyOf(draft, keyProps));
        }
        if (keys.isEmpty()) {
            return new HashMap<>();
        }
        List<ImmutableSpi> entities = findOldList(queryReason, (q, t) -> {
            Expression<Object> keyExpr;
            if (keyProps.size() == 1) {
                keyExpr = t.get(keyProps.iterator().next());
            } else {
                Expression<?>[] arr = new Expression[keyProps.size()];
                int index = 0;
                for (ImmutableProp keyProp : keyProps) {
                    Expression<Object> expr;
                    if (keyProp.isReference(TargetLevel.PERSISTENT)) {
                        expr = t.join(keyProp).get(keyProp.getTargetType().getIdProp());
                    } else {
                        expr = t.get(keyProp);
                    }
                    arr[index++] = expr;
                }
                keyExpr = Tuples.expressionOf(arr);
            }
            q.where(keyExpr.nullableIn(keys));
        });
        if (entities.isEmpty()) {
            return new HashMap<>();
        }
        Map<Object, ImmutableSpi> map = new LinkedHashMap<>((entities.size() * 4 + 2) / 3);
        for (ImmutableSpi entity : entities) {
            ImmutableSpi conflictEntity = map.put(Keys.keyOf(entity, keyProps), entity);
            if (conflictEntity != null) {
                throw new SaveException.KeyNotUnique(
                        ctx.path,
                        "Key properties " +
                                keyProps +
                                " cannot guarantee uniqueness under that path, " +
                                "do you forget to add unique constraint for that key?"
                );
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private List<ImmutableSpi> findOldList(QueryReason queryReason, BiConsumer<MutableQuery, Table<?>> block) {
        ImmutableType type = ctx.path.getType();
        SaveOptions options = ctx.options;
        return Internal.requiresNewDraftContext(draftContext -> {
            List<ImmutableSpi> list = Queries.createQuery(
                    options.getSqlClient(),
                    type,
                    ExecutionPurpose.command(queryReason),
                    FilterLevel.IGNORE_USER_FILTERS,
                    (q, table) -> {
                        block.accept(q, table);
                        if (ctx.trigger != null) {
                            return q.select((Table<ImmutableSpi>)table);
                        }
                        return q.select(
                                ((Table<ImmutableSpi>)table).fetch(
                                        originalFetcher()
                                )
                        );
                    }
            ).forUpdate(options.getLockMode() == LockMode.PESSIMISTIC).execute(ctx.con);
            return draftContext.resolveList(list);
        });
    }

    @SuppressWarnings("unchecked")
    private Fetcher<ImmutableSpi> originalFetcher() {
        Fetcher<ImmutableSpi> oldFetcher = this.originalFetcher;
        if (oldFetcher == null) {
            ImmutableType type = ctx.path.getType();
            FetcherImplementor<ImmutableSpi> fetcherImplementor =
                    new FetcherImpl<>((Class<ImmutableSpi>)ctx.path.getType().getJavaClass());
            for (ImmutableProp keyProp : keyProps) {
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
        if (!hasId) {
            IdGenerator idGenerator = ctx.options.getSqlClient().getIdGenerator(ctx.path.getType().getJavaClass());
            if (idGenerator == null) {
                ctx.throwNoIdGenerator();
            }
            if (!(idGenerator instanceof IdentityIdGenerator)) {
                return QueryReason.IDENTITY_GENERATOR_REQUIRED;
            }
        }
        if (ctx.options.getMode() == SaveMode.UPSERT) {
            if (!sqlClient.getDialect().isUpsertSupported()) {
                return QueryReason.UPSERT_NOT_SUPPORTED;
            }
            boolean useOptimisticLock =
                    ctx.options.getUserOptimisticLock(ctx.path.getType()) != null ||
                            ctx.path.getType().getVersionProp() != null;
            if (useOptimisticLock) {
                return QueryReason.OPTIMISTIC_LOCK;
            }
            if (!hasId) {
                KeyUniqueConstraint constraint =
                        ctx.path.getType().getJavaClass().getAnnotation(KeyUniqueConstraint.class);
                if (constraint == null) {
                    return QueryReason.KEY_UNIQUE_CONSTRAINT_REQUIRED;
                }
                if (!constraint.noMoreUniqueConstraints() &&
                        !sqlClient.getDialect().isUpsertWithMultipleUniqueConstraintSupported()) {
                    return QueryReason.NO_MORE_UNIQUE_CONSTRAINTS_REQUIRED;
                }
                if (!constraint.isNullNotDistinct()) {
                    Set<ImmutableProp> keyProps = ctx.options.getKeyProps(ctx.path.getType());
                    List<PropertyGetter> nullableGetters = new ArrayList<>();
                    for (PropertyGetter getter : Shape.fullOf(sqlClient, ctx.path.getType().getJavaClass()).getGetters()) {
                        if (getter.metadata().isNullable() && keyProps.contains(getter.prop())) {
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
        return QueryReason.NONE;
    }

    final void callInterceptor(DraftSpi draft, ImmutableSpi original) {
        if (original == null && ctx.options.getMode() != SaveMode.UPDATE_ONLY) {
            assignId(draft);
            assignVersion(draft);
            assignLocalDeletedInfo(draft);
        }
        if (interceptor != null) {
            interceptor.beforeSave(draft, original);
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
            SaveMode mode
    ) {
        return createEntityMap(i1, i2, ImmutableProp::isColumnDefinition, mode);
    }

    final ShapedEntityMap<DraftSpi> createEntityMap(
            Iterable<DraftSpi> i1,
            Iterable<DraftSpi> i2,
            Predicate<ImmutableProp> propFilter,
            SaveMode mode
    ) {
        ShapedEntityMap<DraftSpi> entityMap =
                new ShapedEntityMap<>(ctx.options.getSqlClient(), keyProps, propFilter, mode);
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
        return entityMap;
    }

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
                ctx.throwIllegalTargetIds(ids);
            }
        } else {
            MutableRootQueryImpl<Table<Object>> q = new MutableRootQueryImpl<>(
                    ctx.options.getSqlClient(),
                    ctx.path.getType(),
                    ExecutionPurpose.MUTATE,
                    FilterLevel.IGNORE_ALL
            );
            Table<?> table = q.getTableImplementor();
            q.where(table.getId().in(ids));
            List<Object> actualTargetIds = q.select(table.getId()).execute(ctx.con);
            if (actualTargetIds.size() < ids.size()) {
                actualTargetIds.forEach(ids::remove);
                ctx.throwIllegalTargetIds(ids);
            }
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
        for (DraftSpi draft : draftsWithId) {
            callInterceptor(draft, null);
        }
        for (DraftSpi draft : draftsWithKey) {
            callInterceptor(draft, null);
        }

        this.insertedMap = createEntityMap(draftsWithId, draftsWithKey, SaveMode.INSERT_ONLY);
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
                        callInterceptor(draft, original);
                    } else {
                        itr.remove();
                    }
                }
            }
        }

        if (!draftsWithKey.isEmpty()){
            QueryReason queryReason = queryReason(false, draftsWithKey);
            if (queryReason != QueryReason.NONE) {
                Map<Object, ImmutableSpi> keyMap = findOldMapByKeys(queryReason);
                Iterator<DraftSpi> itr = draftsWithKey.iterator();
                while (itr.hasNext()) {
                    DraftSpi draft = itr.next();
                    Object key = Keys.keyOf(draft, keyProps);
                    ImmutableSpi original = keyMap.get(key);
                    if (original != null) {
                        draft.__set(idPropId, original.__get(idPropId));
                        callInterceptor(draft, original);
                    } else {
                        itr.remove();
                    }
                }
            }
        }

        this.updatedMap = createEntityMap(draftsWithId, draftsWithKey, SaveMode.UPDATE_ONLY);
    }
}

class UpsertPreHandler extends AbstractPreHandler {

    private ShapedEntityMap<DraftSpi> insertedMap;

    private ShapedEntityMap<DraftSpi> updatedMap;

    private ShapedEntityMap<DraftSpi> mergedMap;

    UpsertPreHandler(SaveContext ctx) {
        super(ctx);
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
                    if (original != null) {
                        updatedList.add(draft);
                    } else {
                        insertedList.add(draft);
                        itr.remove();
                    }
                    callInterceptor(draft, original);
                }
            }
        }

        if (!draftsWithKey.isEmpty()) {
            QueryReason queryReason = queryReason(false, draftsWithKey);
            if (queryReason != QueryReason.NONE) {
                if (insertedList == null) {
                    insertedList = new ArrayList<>();
                }
                updatedWithoutKeyList = new ArrayList<>();
                Map<Object, ImmutableSpi> keyMap = findOldMapByKeys(queryReason);
                Iterator<DraftSpi> itr = draftsWithKey.iterator();
                while (itr.hasNext()) {
                    DraftSpi draft = itr.next();
                    Object key = Keys.keyOf(draft, keyProps);
                    ImmutableSpi original = keyMap.get(key);
                    if (original != null) {
                        updatedWithoutKeyList.add(draft);
                        draft.__set(idPropId, original.__get(idPropId));
                    } else {
                        insertedList.add(draft);
                        itr.remove();
                    }
                    callInterceptor(draft, original);
                }
            }
        }

        if (insertedList == null) {
            this.insertedMap = ShapedEntityMap.empty();
            this.updatedMap = ShapedEntityMap.empty();
            this.mergedMap = createEntityMap(draftsWithId, draftsWithKey, SaveMode.UPSERT);
        } else {
            this.insertedMap = createEntityMap(insertedList, null, SaveMode.INSERT_ONLY);
            this.updatedMap = createEntityMap(updatedList, null, SaveMode.UPDATE_ONLY);
            if (updatedWithoutKeyList != null && !updatedWithoutKeyList.isEmpty()) {
                ShapedEntityMap<DraftSpi> updatedMap = this.updatedMap;
                for (DraftSpi draft : updatedWithoutKeyList) {
                    updatedMap.add(draft, true);
                }
            }
            this.mergedMap = ShapedEntityMap.empty();
        }
    }
}
