package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.TypeMatchMode;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.IdOnlyFetchType;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;
import java.util.function.Supplier;

class Operator {

    private static final String GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON =
            "Joining is disabled in general optimistic lock";

    private static final int[] SIMPLE_ILLEGAL_ROW_COUNTS = new int[]{-1};

    private static final int[] EMPTY_ROW_COUNTS = new int[0];

    final SaveContext ctx;

    private final boolean ownerAcceptanceRequired;

    Operator(SaveContext ctx) {
        this(ctx, false);
    }

    Operator(SaveContext ctx, boolean ownerAcceptanceRequired) {
        this.ctx = ctx;
        this.ownerAcceptanceRequired = ownerAcceptanceRequired;
    }

    private static int rowCount(int[] rowCounts) {
        int sumRowCount = 0;
        for (int rowCount : rowCounts) {
            if (rowCount != 0) {
                sumRowCount++;
            }
        }
        return sumRowCount;
    }

    public MutationRows insert(Batch<DraftSpi> batch) {
        if (batch.entities().isEmpty()) {
            return MutationRows.EMPTY;
        }
        ImmutableType type = batch.shape().getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                inheritanceInfo.getRootType() != type) {
            return insertJoined(batch, inheritanceInfo);
        }
        insert(
                batch,
                ctx.path.getType(),
                discriminatorProp(inheritanceInfo),
                false
        );
        return MutationRows.accepted(batch.entities());
    }

    private MutationRows insertJoined(Batch<DraftSpi> batch, InheritanceInfo inheritanceInfo) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        DraftSpi sample = batch.entities().iterator().next();
        ImmutableType rootType = inheritanceInfo.getRootType();
        Shape rootShape = joinedRootShape(sqlClient, rootType, sample);
        insertJoinedRoot(batchOf(batch, rootShape), inheritanceInfo);
        ImmutableType previousTableType = rootType;
        for (ImmutableType tableType : joinedTableTypes(rootType, batch.shape().getType())) {
            Shape shape = joinedStageShape(sqlClient, previousTableType, tableType, sample);
            insertJoinedStage(batchOf(batch, shape), tableType);
            previousTableType = tableType;
        }
        return MutationRows.accepted(batch.entities());
    }

    void insertJoinedRoot(Batch<DraftSpi> batch, InheritanceInfo inheritanceInfo) {
        insert(
                batch,
                inheritanceInfo.getRootType(),
                discriminatorProp(inheritanceInfo),
                true
        );
    }

    void insertJoinedStage(Batch<DraftSpi> batch, ImmutableType tableType) {
        insert(batch, tableType, null, true);
    }

    static Shape joinedRootShape(
            JSqlClientImplementor sqlClient,
            ImmutableType rootType,
            DraftSpi sample
    ) {
        InheritanceInfo inheritanceInfo = rootType.getInheritanceInfo();
        return Shape.of(
                sqlClient,
                rootType,
                sample,
                prop -> inheritanceInfo == null || inheritanceInfo.isPropAvailableInTable(prop, rootType)
        );
    }

    static Shape joinedStageShape(
            JSqlClientImplementor sqlClient,
            ImmutableType parentTableType,
            ImmutableType tableType,
            DraftSpi sample
    ) {
        InheritanceInfo inheritanceInfo = tableType.getInheritanceInfo();
        return Shape.of(
                sqlClient,
                tableType,
                sample,
                prop -> prop.isId() ||
                        (prop.isColumnDefinition() &&
                                (inheritanceInfo == null ||
                                        (inheritanceInfo.isPropAvailableInTable(prop, tableType) &&
                                                !inheritanceInfo.isPropAvailableInTable(prop, parentTableType))))
        );
    }

    private void insert(
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            boolean allowIdOnly
    ) {
        insert(batch, tableType, discriminatorProp, allowIdOnly, true);
    }

    private void insert(
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            boolean allowIdOnly,
            boolean fireTrigger
    ) {

        if (batch.entities().isEmpty() || (!allowIdOnly && batch.shape().isIdOnly())) {
            return;
        }
        validate(batch.shape(), true, implicitKeyProps(null));

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        PropertyGetter discriminatorGetter = discriminatorProp != null ?
                PropertyGetter.propertyGetters(sqlClient, discriminatorProp).get(0) :
                null;
        List<PropertyGetter> defaultGetters = new ArrayList<>();
        for (PropertyGetter getter : Shape.fullOf(sqlClient, tableType.getJavaClass()).getGetters()) {
            if (getter.metadata().hasDefaultValue() && !batch.shape().contains(getter)) {
                defaultGetters.add(getter);
            }
        }
        IdentityIdGenerator identityIdGenerator = null;
        SequenceIdGenerator sequenceIdGenerator = null;
        UserIdGenerator<?> userIdGenerator = null;
        if (batch.shape().getIdGetters().isEmpty()) {
            IdGenerator idGenerator = sqlClient.getIdGenerator(tableType.getJavaClass());
            if (idGenerator instanceof SequenceIdGenerator) {
                sequenceIdGenerator = (SequenceIdGenerator) idGenerator;
            } else if (idGenerator instanceof UserIdGenerator<?>) {
                userIdGenerator = (UserIdGenerator<?>) idGenerator;
            } else if (idGenerator instanceof IdentityIdGenerator) {
                identityIdGenerator = (IdentityIdGenerator) idGenerator;
            } else {
                ctx.throwIllegalIdGenerator(
                        "In order to insert object without id, the id generator must be identity or sequence"
                );
            }
        }

        if (userIdGenerator != null) {
            Class<?> javaType = tableType.getJavaClass();
            PropId idPropId = tableType.getIdProp().getId();
            for (DraftSpi draft : batch.entities()) {
                Object id = userIdGenerator.generate(javaType);
                if (id == null || id.getClass() != tableType.getIdProp().getReturnClass()) {
                    ctx.throwIllegalGeneratedId(id);
                }
                draft.__set(idPropId, id);
            }
        }

        UpsertMask<?> upsertMask;
        List<ImmutableProp> conflictProps;
        if (batch.originalMode() == SaveMode.UPSERT) {
            upsertMask = ctx.options.getUpsertMask(tableType);
            if (!batch.shape().getIdGetters().isEmpty()) {
                conflictProps = Collections.singletonList(batch.shape().getType().getIdProp());
            } else {
                Set<ImmutableProp> keyProps = batch.shape().keyProps(
                        ctx.options.getKeyMatcher(tableType),
                        implicitKeyProps(null)
                );
                conflictProps = MutationKeys.keyAndLogicalDeletedProps(batch.shape().getType(), keyProps);
            }
        } else {
            upsertMask = null;
            conflictProps = Collections.emptyList();
        }

        List<PropertyGetter> insertedGetters = new ArrayList<>();
        for (PropertyGetter getter : batch.shape().getGetters()) {
            if (getter.prop().isColumnDefinition() &&
                    getter.isInsertable(conflictProps, upsertMask)) {
                insertedGetters.add(getter);
            }
        }

        SaveReturning returning =
                userIdGenerator == null ?
                        SaveReturning.forInsert(
                                ctx,
                                batch.shape(),
                                batch.entities(),
                                tableType,
                                sequenceIdGenerator,
                                identityIdGenerator != null || sequenceIdGenerator != null,
                                insertedGetters,
                                discriminatorGetter,
                                defaultGetters
                        ) :
                        null;
        if (returning != null) {
            int rowCount = rowCount(returning.executeInsert(batch.entities()));
            completeInsertedFetcherFields(batch);
            MutationTrigger trigger = fireTrigger ? ctx.trigger : null;
            if (trigger != null) {
                for (DraftSpi draft : batch.entities()) {
                    trigger.modifyEntityTable(null, draft);
                }
            }
            AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount);
            return;
        }

        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        BatchSqlBuilder builder = new BatchSqlBuilder(
                sqlClient,
                batch.entities().size() < 2 || ctx.options.isBatchForbidden()
        );
        builder.sql("insert into ")
                .sql(tableType.getTableName(strategy))
                .enter(BatchSqlBuilder.ScopeType.TUPLE);
        if (sequenceIdGenerator != null) {
            builder.separator().sql(tableType.getIdProp().<SingleColumn>getStorage(strategy).getName());
        }
        for (PropertyGetter getter : insertedGetters) {
            if (getter.prop().isId() && getter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().sql(getter);
            }
        }
        if (discriminatorGetter != null) {
            builder.separator().sql(discriminatorGetter);
        }
        for (PropertyGetter getter : insertedGetters) {
            if (!getter.prop().isId() &&
                    getter.prop().isColumnDefinition() &&
                    getter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().sql(getter);
            }
        }
        for (PropertyGetter defaultGetter : defaultGetters) {
            if (defaultGetter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().sql(defaultGetter);
            }
        }
        builder.leave().sql(" values").enter(BatchSqlBuilder.ScopeType.TUPLE);
        if (sequenceIdGenerator != null) {
            builder.separator()
                    .sql("(")
                    .sql(
                            sqlClient.getDialect().getSelectIdFromSequenceSql(sequenceIdGenerator.getSequenceName())
                    )
                    .sql(")");
        } else if (userIdGenerator != null) {
            Shape fullShape = Shape.fullOf(sqlClient, tableType.getJavaClass());
            builder.separator();
            for (PropertyGetter getter : fullShape.getIdGetters()) {
                builder.separator().sql(getter);
            }
        }
        for (PropertyGetter getter : insertedGetters) {
            if (getter.prop().isId() && getter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().variable(getter);
            }
        }
        if (discriminatorGetter != null) {
            builder.separator().variable(discriminatorGetter);
        }
        for (PropertyGetter getter : insertedGetters) {
            if (!getter.prop().isId() &&
                    getter.prop().isColumnDefinition() &&
                    getter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().variable(getter);
            }
        }
        for (PropertyGetter defaultGetter : defaultGetters) {
            if (defaultGetter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().defaultVariable(defaultGetter);
            }
        }
        builder.leave();
        if ((identityIdGenerator != null || sequenceIdGenerator != null) &&
                sqlClient.getDialect().isInsertedIdReturningRequired()) {
            builder.sql(" returning ")
                    .sql(
                            tableType.getIdProp()
                                    .<SingleColumn>getStorage(sqlClient.getMetadataStrategy())
                                    .getName()
                    );
        }

        int rowCount = execute(builder, batch, false, false);
        completeInsertedFetcherFields(batch);
        // Fire the trigger after `execute` so the draft already reflects its final,
        // post-execution state (generated id, version, ...) before being captured.
        MutationTrigger trigger = fireTrigger ? ctx.trigger : null;
        if (trigger != null) {
            for (DraftSpi draft : batch.entities()) {
                trigger.modifyEntityTable(null, draft);
            }
        }
        AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount);
    }

    private void completeInsertedFetcherFields(Batch<DraftSpi> batch) {
        Fetcher<?> fetcher = ctx.fetcher;
        if (fetcher == null || ctx.path.getParent() != null) {
            return;
        }
        SaveFetcherAnalysis analysis = SaveFetcherAnalysis.of(fetcher, batch.shape().getType());
        if (analysis.hasTypeBranches()) {
            return;
        }
        List<ImmutableProp> props = analysis.getCompletableProps();
        if (props.isEmpty()) {
            return;
        }
        for (EntityCollection.Item<DraftSpi> item : batch.entities().items()) {
            completeInsertedFetcherFields(item.getEntity(), props);
            for (DraftSpi draft : item.getOriginalEntities()) {
                if (draft != item.getEntity()) {
                    completeInsertedFetcherFields(draft, props);
                }
            }
        }
    }

    private static void completeInsertedFetcherFields(DraftSpi draft, List<ImmutableProp> props) {
        for (ImmutableProp prop : props) {
            PropId propId = prop.getId();
            if (draft.__isLoaded(propId)) {
                continue;
            }
            Ref<Object> defaultRef = prop.getDefaultValueRef();
            if (defaultRef != null) {
                Object value = defaultRef.getValue();
                draft.__set(propId, value instanceof Supplier<?> ? ((Supplier<?>) value).get() : value);
            } else {
                draft.__set(propId, null);
            }
        }
    }

    private ImmutableProp discriminatorProp(@Nullable InheritanceInfo inheritanceInfo) {
        if (inheritanceInfo == null) {
            return null;
        }
        return inheritanceInfo.getDiscriminatorProp();
    }

    private ImmutableProp discriminatorGuardProp(@Nullable InheritanceInfo inheritanceInfo, ImmutableType type) {
        if (inheritanceInfo == null) {
            return null;
        }
        TypeMatchMode resolvedMode = TypeMatchModes.resolve(type, ctx.options.getTypeMatchMode(type));
        if (resolvedMode == TypeMatchMode.POLYMORPHIC) {
            if (inheritanceInfo.getRootType() == type) {
                return null;
            }
            Collection<ImmutableType> concreteTypes = inheritanceInfo.getConcreteTypes(type);
            if (concreteTypes.size() == 1 && concreteTypes.iterator().next() == type) {
                return inheritanceInfo.getDiscriminatorProp();
            }
            throw new ExecutionException(
                    "Cannot save inheritance entity type \"" +
                            type +
                            "\" with " +
                            TypeMatchMode.POLYMORPHIC +
                            " type match mode because polymorphic non-root save/update is not supported yet"
            );
        }
        if (!type.isInstantiable()) {
            throw new ExecutionException(
                    "Cannot save inheritance entity type \"" +
                            type +
                            "\" exactly because it is abstract"
            );
        }
        return inheritanceInfo.getDiscriminatorProp();
    }

    private static Collection<ImmutableProp> implicitKeyProps(@Nullable ImmutableProp discriminatorGuardProp) {
        if (discriminatorGuardProp != null) {
            return Collections.singleton(discriminatorGuardProp);
        }
        return Collections.emptySet();
    }

    static List<ImmutableType> joinedTableTypes(ImmutableType rootType, ImmutableType type) {
        List<ImmutableType> tableTypes = new ArrayList<>();
        for (ImmutableType t = type; t != rootType; t = t.getPrimarySuperType()) {
            if (t.isEntity()) {
                tableTypes.add(t);
            }
        }
        Collections.reverse(tableTypes);
        return tableTypes;
    }

    private static Batch<DraftSpi> batchOfChangedRows(Batch<DraftSpi> base, int[] rowCounts) {
        if (rowCounts.length == 0) {
            return batchOf(base, base.shape(), new EntityList<>());
        }
        EntityList<DraftSpi> entities = new EntityList<>();
        int index = 0;
        for (EntityCollection.Item<DraftSpi> item : base.entities().items()) {
            if (index < rowCounts.length && rowCounts[index++] != 0) {
                entities.add(item.getEntity());
            }
        }
        return batchOf(base, base.shape(), entities);
    }

    private static EntityList<DraftSpi> acceptedOriginalEntities(Batch<DraftSpi> base, int[] rowCounts) {
        EntityList<DraftSpi> entities = new EntityList<>();
        if (rowCounts.length == 0) {
            return entities;
        }
        int index = 0;
        for (EntityCollection.Item<DraftSpi> item : base.entities().items()) {
            if (index < rowCounts.length && rowCounts[index++] != 0) {
                for (DraftSpi draft : item.getOriginalEntities()) {
                    entities.add(draft);
                }
            }
        }
        return entities;
    }

    private static Batch<DraftSpi> batchOfRows(Batch<DraftSpi> base, Set<Object> ids) {
        if (ids.isEmpty()) {
            return batchOf(base, base.shape(), new EntityList<>());
        }
        EntityList<DraftSpi> entities = new EntityList<>();
        PropId idPropId = base.shape().getType().getIdProp().getId();
        for (EntityCollection.Item<DraftSpi> item : base.entities().items()) {
            if (ids.contains(item.getEntity().__get(idPropId))) {
                entities.add(item.getEntity());
            }
        }
        return batchOf(base, base.shape(), entities);
    }

    private static Batch<DraftSpi> batchOfRowsNotIn(Batch<DraftSpi> base, Set<Object> ids) {
        EntityList<DraftSpi> entities = new EntityList<>();
        PropId idPropId = base.shape().getType().getIdProp().getId();
        for (EntityCollection.Item<DraftSpi> item : base.entities().items()) {
            Object id = item.getEntity().__get(idPropId);
            if (id == null || !ids.contains(id)) {
                entities.add(item.getEntity());
            }
        }
        return batchOf(base, base.shape(), entities);
    }

    private static EntityList<DraftSpi> acceptedOriginalEntities(Batch<DraftSpi> base, Set<Object> ids) {
        EntityList<DraftSpi> entities = new EntityList<>();
        if (ids.isEmpty()) {
            return entities;
        }
        PropId idPropId = base.shape().getType().getIdProp().getId();
        for (EntityCollection.Item<DraftSpi> item : base.entities().items()) {
            if (ids.contains(item.getEntity().__get(idPropId))) {
                for (DraftSpi draft : item.getOriginalEntities()) {
                    entities.add(draft);
                }
            }
        }
        return entities;
    }

    private static Batch<DraftSpi> batchOf(Batch<DraftSpi> base, Shape shape) {
        return batchOf(base, shape, base.entities());
    }

    private static Batch<DraftSpi> batchOf(Batch<DraftSpi> base, Shape shape, EntityCollection<DraftSpi> entities) {
        return new Batch<DraftSpi>() {

            @Override
            public Shape shape() {
                return shape;
            }

            @Override
            public EntityCollection<DraftSpi> entities() {
                return entities;
            }

            @Override
            public SaveMode mode() {
                return base.mode();
            }

            @Override
            public SaveMode originalMode() {
                return base.originalMode();
            }
        };
    }

    private static int compareJoinedCleanupTableTypes(
            MetadataStrategy strategy,
            ImmutableType a,
            ImmutableType b
    ) {
        int cmp = Integer.compare(b.getAllTypes().size(), a.getAllTypes().size());
        if (cmp != 0) {
            return cmp;
        }
        cmp = a.getTableName(strategy).compareTo(b.getTableName(strategy));
        if (cmp != 0) {
            return cmp;
        }
        return a.getJavaClass().getName().compareTo(b.getJavaClass().getName());
    }

    public MutationRows update(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch
    ) {
        if (batch.entities().isEmpty()) {
            return MutationRows.EMPTY;
        }
        ImmutableType type = batch.shape().getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        boolean typeChangeAllowed = ctx.options.isTypeChangeAllowed(type);
        if (inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                (inheritanceInfo.getRootType() != type || typeChangeAllowed)) {
            return updateJoined(originalIdObjMap, originalKeyObjMap, batch, inheritanceInfo);
        }
        update(
                originalIdObjMap,
                originalKeyObjMap,
                batch,
                ctx.path.getType(),
                typeChangeAllowed ? discriminatorProp(inheritanceInfo) : null,
                typeChangeAllowed ? null : discriminatorGuardProp(inheritanceInfo, type),
                typeChangeAllowed ? redundantSingleTableGetters(inheritanceInfo, type) : Collections.emptyList(),
                false,
                false
        );
        return MutationRows.UNKNOWN;
    }

    private MutationRows updateJoined(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch,
            InheritanceInfo inheritanceInfo
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        DraftSpi sample = batch.entities().iterator().next();
        ImmutableType rootType = inheritanceInfo.getRootType();
        Shape rootShape = joinedRootShape(sqlClient, rootType, sample);
        Batch<DraftSpi> rootBatch = batchOf(batch, rootShape);
        boolean typeChangeAllowed = ctx.options.isTypeChangeAllowed(batch.shape().getType());
        if (typeChangeAllowed && rootShape.getIdGetters().isEmpty()) {
            fillIds(QueryReason.GET_ID_FOR_KEY_BASE_UPDATE, originalKeyObjMap, rootBatch);
            if (rootBatch.entities().isEmpty()) {
                return MutationRows.EMPTY;
            }
            sample = rootBatch.entities().iterator().next();
            rootShape = joinedRootShape(sqlClient, rootType, sample);
            rootBatch = batchOf(batch, rootShape);
        } else if (!typeChangeAllowed &&
                rootShape.getIdGetters().isEmpty() &&
                !sqlClient.getDialect().isIdFetchableByKeyUpdate()) {
            fillIds(QueryReason.GET_ID_FOR_KEY_BASE_UPDATE, originalKeyObjMap, rootBatch);
            if (rootBatch.entities().isEmpty()) {
                return MutationRows.EMPTY;
            }
            sample = rootBatch.entities().iterator().next();
            rootShape = joinedRootShape(sqlClient, rootType, sample);
            rootBatch = batchOf(batch, rootShape);
        }
        Map<Object, ImmutableSpi> typeChangeOldRowMap =
                typeChangeAllowed && ctx.trigger != null ?
                        findTypeChangeOldRows(rootBatch, inheritanceInfo) :
                        Collections.emptyMap();
        TypeChangeRows typeChangeRows = typeChangeAllowed ?
                (
                        ctx.trigger != null ?
                                typeChangeRows(typeChangeOldRowMap.values()) :
                                resolveOldTypeForChange(rootBatch, inheritanceInfo)
                ) :
                null;
        Batch<DraftSpi> acceptanceBatch = rootBatch;
        Set<Object> typeChangeIds = typeChangeRows != null ?
                typeChangeRows.ids() :
                Collections.emptySet();
        Set<Object> acceptedTypeChangeIds = null;
        TypeChangeRows acceptedTypeChangeRows = typeChangeRows;
        if (typeChangeAllowed) {
            if (hasMissingTypeChangeRow(rootBatch, typeChangeIds) && isOptimisticLockActive(rootShape)) {
                throwOptimisticLockErrorForMissingTypeChangeRow(rootBatch, typeChangeIds);
            }
            acceptedTypeChangeIds = updateRootForTypeChange(
                    originalIdObjMap,
                    originalKeyObjMap,
                    rootBatch,
                    rootType,
                    batch.shape().getType(),
                    inheritanceInfo,
                    typeChangeRows
            );
            fireTypeChangeTriggers(batch, typeChangeOldRowMap, acceptedTypeChangeIds);
            acceptedTypeChangeRows = typeChangeRows != null ?
                    typeChangeRows.filteredBy(acceptedTypeChangeIds) :
                    null;
            rootBatch = batchOfRows(rootBatch, acceptedTypeChangeIds);
            batch = batchOfRows(batch, acceptedTypeChangeIds);
            if (batch.entities().isEmpty()) {
                return MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, acceptedTypeChangeIds));
            }
            sample = batch.entities().iterator().next();
        }
        boolean forceRootOneByOne =
                !typeChangeAllowed &&
                        ownerAcceptanceRequired &&
                        sqlClient.getDialect().isBatchDumb();
        int[] rootRowCounts = EMPTY_ROW_COUNTS;
        if (!typeChangeAllowed) {
            rootRowCounts = update(
                    originalIdObjMap,
                    originalKeyObjMap,
                    rootBatch,
                    rootType,
                    null,
                    discriminatorProp(inheritanceInfo),
                    Collections.emptyList(),
                    ownerAcceptanceRequired,
                    forceRootOneByOne
            );
        }
        boolean rootRowCountsReliable = false;
        if (typeChangeAllowed) {
            deleteRedundantJoinedRows(batch, inheritanceInfo, acceptedTypeChangeRows);
        } else {
            rootRowCountsReliable =
                    rootRowCounts.length != 0 &&
                            (!sqlClient.getDialect().isBatchDumb() || forceRootOneByOne);
            if (rootRowCountsReliable) {
                batch = batchOfChangedRows(batch, rootRowCounts);
                if (batch.entities().isEmpty()) {
                    return MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, rootRowCounts));
                }
                sample = batch.entities().iterator().next();
            }
        }
        ImmutableType previousTableType = rootType;
        for (ImmutableType tableType : joinedTableTypes(rootType, batch.shape().getType())) {
            Shape shape = joinedStageShape(sqlClient, previousTableType, tableType, sample);
            Batch<DraftSpi> childBatch = batchOf(batch, shape);
            if (typeChangeAllowed) {
                saveJoinedTableForTypeChange(
                        childBatch,
                        tableType,
                        rootType,
                        acceptedTypeChangeRows
                );
            } else {
                updateJoinedChildWithRootGuard(childBatch, tableType, rootType, inheritanceInfo);
            }
            previousTableType = tableType;
        }
        return typeChangeAllowed ?
                MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, acceptedTypeChangeIds)) :
                (rootRowCountsReliable ?
                        MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, rootRowCounts)) :
                        MutationRows.UNKNOWN);
    }

    private Set<Object> updateRootForTypeChange(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> rootBatch,
            ImmutableType rootType,
            ImmutableType targetType,
            InheritanceInfo inheritanceInfo,
            @Nullable TypeChangeRows typeChangeRows
    ) {
        if (typeChangeRows == null || typeChangeRows.oldTypeIdMap.isEmpty()) {
            return Collections.emptySet();
        }
        ImmutableProp discriminatorProp = discriminatorProp(inheritanceInfo);
        Set<Object> acceptedIds = new LinkedHashSet<>();
        boolean forceOneByOne = ctx.options.getSqlClient().getDialect().isBatchDumb();
        for (Map.Entry<ImmutableType, Set<Object>> e : typeChangeRows.oldTypeIdMap.entrySet()) {
            ImmutableType oldType = e.getKey();
            Batch<DraftSpi> groupBatch = batchOfRows(rootBatch, e.getValue());
            int[] rowCounts = update(
                    originalIdObjMap,
                    originalKeyObjMap,
                    groupBatch,
                    rootType,
                    oldType != targetType ? discriminatorProp : null,
                    discriminatorProp,
                    DiscriminatorValues.of(oldType),
                    Collections.emptyList(),
                    true,
                    forceOneByOne,
                    false
            );
            collectAcceptedIds(acceptedIds, groupBatch, rowCounts);
        }
        return acceptedIds;
    }

    private static void collectAcceptedIds(Set<Object> output, Batch<DraftSpi> batch, int[] rowCounts) {
        PropId idPropId = batch.shape().getType().getIdProp().getId();
        int index = 0;
        for (EntityCollection.Item<DraftSpi> item : batch.entities().items()) {
            if (index < rowCounts.length && rowCounts[index++] != 0) {
                Object id = item.getEntity().__get(idPropId);
                if (id != null) {
                    output.add(id);
                }
            }
        }
    }

    private static void collectIds(Set<Object> output, Batch<DraftSpi> batch) {
        PropId idPropId = batch.shape().getType().getIdProp().getId();
        for (DraftSpi draft : batch.entities()) {
            Object id = draft.__get(idPropId);
            if (id != null) {
                output.add(id);
            }
        }
    }

    private Map<Object, ImmutableSpi> findTypeChangeOldRows(
            Batch<DraftSpi> rootBatch,
            InheritanceInfo inheritanceInfo
    ) {
        Set<Object> ids = new LinkedHashSet<>((rootBatch.entities().size() * 4 + 2) / 3);
        PropId idPropId = inheritanceInfo.getRootType().getIdProp().getId();
        for (DraftSpi draft : rootBatch.entities()) {
            Object id = draft.__get(idPropId);
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Map<Object, ImmutableSpi> oldRowMap = new LinkedHashMap<>();
        List<ImmutableSpi> oldRows = PolymorphicEntityReadPlan.readByIds(
                sqlClient,
                ctx.con,
                inheritanceInfo.getRootType(),
                QueryReason.TRIGGER,
                ids,
                ctx.options.getExceptionTranslator()
        );
        for (ImmutableSpi oldRow : oldRows) {
            oldRowMap.put(oldRow.__get(oldRow.__type().getIdProp().getId()), oldRow);
        }
        return oldRowMap;
    }

    private TypeChangeRows typeChangeRows(Collection<ImmutableSpi> oldRows) {
        if (oldRows.isEmpty()) {
            return TypeChangeRows.EMPTY;
        }
        Map<ImmutableType, Set<Object>> oldTypeIdMap = new LinkedHashMap<>();
        for (ImmutableSpi oldRow : oldRows) {
            oldTypeIdMap
                    .computeIfAbsent(oldRow.__type(), it -> new LinkedHashSet<>())
                    .add(oldRow.__get(oldRow.__type().getIdProp().getId()));
        }
        return new TypeChangeRows(oldTypeIdMap);
    }

    private void fireTypeChangeTriggers(
            Batch<DraftSpi> batch,
            Map<Object, ImmutableSpi> oldRowMap,
            Set<Object> acceptedIds
    ) {
        MutationTrigger trigger = ctx.trigger;
        if (trigger == null || acceptedIds.isEmpty()) {
            return;
        }
        PropId idPropId = batch.shape().getType().getIdProp().getId();
        for (DraftSpi draft : batch.entities()) {
            Object id = draft.__get(idPropId);
            if (!acceptedIds.contains(id)) {
                continue;
            }
            ImmutableSpi oldRow = oldRowMap.get(id);
            if (isTypeChangeEventRequired(oldRow, draft)) {
                trigger.modifyEntityTable(oldRow, draft);
            }
        }
    }

    private boolean isTypeChangeEventRequired(@Nullable ImmutableSpi oldRow, DraftSpi newRow) {
        if (oldRow == null || oldRow.__type() != newRow.__type()) {
            return true;
        }
        for (ImmutableProp prop : newRow.__type().getProps().values()) {
            if (prop.isId() || !prop.isColumnDefinition()) {
                continue;
            }
            PropId propId = prop.getId();
            if (!newRow.__isLoaded(propId)) {
                continue;
            }
            if (!oldRow.__isLoaded(propId)) {
                return true;
            }
            if (!Objects.equals(oldRow.__get(propId), newRow.__get(propId))) {
                return true;
            }
        }
        return false;
    }

    private boolean isOptimisticLockActive(Shape rootShape) {
        if (ctx.options.getUserOptimisticLock(ctx.path.getType()) != null) {
            userLockOptimisticPredicate();
            return true;
        }
        return rootShape.getVersionGetter() != null;
    }

    private boolean hasMissingTypeChangeRow(Batch<DraftSpi> rootBatch, Set<Object> acceptedIds) {
        PropId idPropId = rootBatch.shape().getType().getIdProp().getId();
        for (DraftSpi draft : rootBatch.entities()) {
            if (!acceptedIds.contains(draft.__get(idPropId))) {
                return true;
            }
        }
        return false;
    }

    private void throwOptimisticLockErrorForMissingTypeChangeRow(Batch<DraftSpi> rootBatch, Set<Object> acceptedIds) {
        PropId idPropId = rootBatch.shape().getType().getIdProp().getId();
        for (DraftSpi draft : rootBatch.entities()) {
            if (!acceptedIds.contains(draft.__get(idPropId))) {
                ctx.throwOptimisticLockError(draft);
            }
        }
    }

    private void saveJoinedTableForTypeChange(
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            ImmutableType rootType,
            @Nullable TypeChangeRows typeChangeRows
    ) {
        if (ctx.trigger == null && ctx.options.getSqlClient().getDialect().isUpsertSupported()) {
            upsert(batch, tableType, null, false, null, Collections.emptyList(), false, false);
            return;
        }
        Set<Object> existingIds = oldTypeJoinedTableIds(rootType, tableType, typeChangeRows);
        EntityList<DraftSpi> existingEntities = new EntityList<>();
        EntityList<DraftSpi> missingEntities = new EntityList<>();
        PropId idPropId = tableType.getIdProp().getId();
        for (DraftSpi draft : batch.entities()) {
            if (existingIds.contains(draft.__get(idPropId))) {
                existingEntities.add(draft);
            } else {
                missingEntities.add(draft);
            }
        }
        if (!existingEntities.isEmpty()) {
            update(
                    null,
                    null,
                    batchOf(batch, batch.shape(), existingEntities),
                    tableType,
                    null,
                    null,
                    null,
                    Collections.emptyList(),
                    false,
                    false,
                    false
            );
        }
        if (!missingEntities.isEmpty()) {
            insert(batchOf(batch, batch.shape(), missingEntities), tableType, null, true, false);
        }
    }

    private Set<Object> oldTypeJoinedTableIds(
            ImmutableType rootType,
            ImmutableType tableType,
            @Nullable TypeChangeRows typeChangeRows
    ) {
        if (typeChangeRows == null || typeChangeRows.oldTypeIdMap.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Object> ids = new LinkedHashSet<>();
        for (Map.Entry<ImmutableType, Set<Object>> e : typeChangeRows.oldTypeIdMap.entrySet()) {
            if (joinedTableTypes(rootType, e.getKey()).contains(tableType)) {
                ids.addAll(e.getValue());
            }
        }
        return ids;
    }

    private TypeChangeRows resolveOldTypeForChange(Batch<DraftSpi> rootBatch, InheritanceInfo inheritanceInfo) {
        ImmutableType rootType = inheritanceInfo.getRootType();
        Set<Object> ids = new LinkedHashSet<>((rootBatch.entities().size() * 4 + 2) / 3);
        PropId idPropId = rootType.getIdProp().getId();
        for (DraftSpi draft : rootBatch.entities()) {
            Object id = draft.__get(idPropId);
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return TypeChangeRows.EMPTY;
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        ImmutableProp idProp = rootType.getIdProp();
        ImmutableProp discriminatorProp = inheritanceInfo.getDiscriminatorProp();
        String discriminatorColumnName = discriminatorProp.<SingleColumn>getStorage(strategy).getName();
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder.sql("select ")
                .definition(idProp.getStorage(strategy))
                .sql(", ")
                .sql(discriminatorColumnName)
                .sql(" from ")
                .sql(rootType.getTableName(strategy))
                .sql(" where ");
        ComparisonPredicates.renderIn(
                false,
                ValueGetter.valueGetters(sqlClient, idProp),
                ids,
                builder
        );
        builder.sql(" order by ")
                .definition(idProp.getStorage(strategy));
        Tuple3<String, List<Object>, List<Integer>> tuple = builder.build();
        Reader<?> idReader = sqlClient.getReader(idProp);
        Reader<?> discriminatorReader = sqlClient.getReader(discriminatorProp);
        Map<Object, ImmutableType> discriminatorTypeMap = inheritanceInfo.getDiscriminatorTypeMap();
        return sqlClient.getExecutor().execute(
                new Executor.Args<>(
                        sqlClient,
                        ctx.con,
                        tuple.get_1(),
                        tuple.get_2(),
                        tuple.get_3(),
                        ExecutionPurpose.command(QueryReason.RESOLVE_OLD_TYPE_FOR_CHANGE),
                        ctx.options.getExceptionTranslator(),
                        null,
                        (stmt, args) -> {
                            Map<ImmutableType, Set<Object>> oldTypeIdMap = new LinkedHashMap<>();
                            Reader.Context readerContext = new Reader.Context(null, sqlClient);
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    readerContext.resetCol();
                                    Object id = idReader.read(rs, readerContext);
                                    Object discriminator = discriminatorReader.read(rs, readerContext);
                                    ImmutableType oldType = discriminatorTypeMap.get(discriminator);
                                    if (oldType == null) {
                                        throw new ExecutionException(
                                                "Cannot change type for joined inheritance rows, " +
                                                        "the discriminator value \"" +
                                                        discriminator +
                                                        "\" of column \"" +
                                                        discriminatorColumnName +
                                                        "\" is not mapped by \"" +
                                                        rootType +
                                                        "\""
                                        );
                                    }
                                    oldTypeIdMap
                                            .computeIfAbsent(oldType, it -> new LinkedHashSet<>())
                                            .add(id);
                                }
                            }
                            return oldTypeIdMap.isEmpty() ?
                                    TypeChangeRows.EMPTY :
                                    new TypeChangeRows(oldTypeIdMap);
                        }
                )
        );
    }

    private int[] update(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            @Nullable ImmutableProp discriminatorGuardProp,
            List<PropertyGetter> nullGetters,
            boolean forceAllRows,
            boolean forceOneByOne
    ) {
        return update(
                originalIdObjMap,
                originalKeyObjMap,
                batch,
                tableType,
                discriminatorProp,
                discriminatorGuardProp,
                null,
                nullGetters,
                forceAllRows,
                forceOneByOne,
                true
        );
    }

    private int[] update(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            @Nullable ImmutableProp discriminatorGuardProp,
            @Nullable Object discriminatorGuardValue,
            List<PropertyGetter> nullGetters,
            boolean forceAllRows,
            boolean forceOneByOne
    ) {
        return update(
                originalIdObjMap,
                originalKeyObjMap,
                batch,
                tableType,
                discriminatorProp,
                discriminatorGuardProp,
                discriminatorGuardValue,
                nullGetters,
                forceAllRows,
                forceOneByOne,
                true
        );
    }

    private int[] update(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            @Nullable ImmutableProp discriminatorGuardProp,
            @Nullable Object discriminatorGuardValue,
            List<PropertyGetter> nullGetters,
            boolean forceAllRows,
            boolean forceOneByOne,
            boolean fireTrigger
    ) {
        Shape shape = batch.shape();
        Collection<ImmutableProp> implicitKeyProps = implicitKeyProps(discriminatorGuardProp);
        validate(shape, false, implicitKeyProps);
        KeyMatcher.Group group = shape.getIdGetters().isEmpty() ?
                shape.group(ctx.options.getKeyMatcher(shape.getType()), implicitKeyProps) :
                null;
        Set<ImmutableProp> keyProps = group != null ? group.getProps() : null;
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        List<PropertyGetter> idGetters = Shape.fullOf(sqlClient, shape.getType().getJavaClass()).getIdGetters();
        if (group != null && idGetters.size() > 1) {
            throw new IllegalArgumentException(
                    "Cannot update batch whose shape does not have id " +
                            "when id property is embeddable"
            );
        }
        Predicate userOptimisticLockPredicate = userLockOptimisticPredicate();
        PropertyGetter versionGetter = shape.getVersionGetter();
        boolean hasOptimisticLock = userOptimisticLockPredicate != null || versionGetter != null;
        if (hasOptimisticLock && keyProps != null) {
            throw new IllegalArgumentException(
                    "Cannot update batch whose shape does not have id " +
                            "when optimistic lock is required"
            );
        }

        if (batch.entities().isEmpty()) {
            return EMPTY_ROW_COUNTS;
        }

        if (!forceAllRows &&
                !hasOptimisticLock &&
                ctx.options.isIdOnlyAsReference(ctx.path.getProp()) &&
                ctx.options.getUnloadedVersionBehavior(shape.getType()) == UnloadedVersionBehavior.IGNORE &&
                shape.isIdOnly()) {
            return EMPTY_ROW_COUNTS;
        }

        Set<ImmutableProp> changedProps =
                originalIdObjMap != null || originalKeyObjMap != null ?
                        new LinkedHashSet<>() :
                        null;
        UpsertMask<?> upsertMask;
        List<ImmutableProp> conflictProps;
        if (batch.originalMode() == SaveMode.UPSERT) {
            upsertMask = ctx.options.getUpsertMask(ctx.path.getType());
            if (!batch.shape().getIdGetters().isEmpty()) {
                conflictProps = Collections.singletonList(batch.shape().getType().getIdProp());
            } else {
                conflictProps = MutationKeys.keyAndLogicalDeletedProps(batch.shape().getType(), keyProps);
            }
        } else {
            upsertMask = null;
            conflictProps = Collections.emptyList();
        }

        List<PropertyGetter> updatedGetters = new ArrayList<>();
        for (PropertyGetter getter : shape.getGetters()) {
            if (!getter.isUpdatable(conflictProps, upsertMask)) {
                continue;
            }
            ImmutableProp prop = getter.prop();
            if (prop.isId()) {
                continue;
            }
            if (prop.isVersion() && userOptimisticLockPredicate == null) {
                continue;
            }
            if (!prop.isColumnDefinition()) {
                continue;
            }
            if (keyProps != null && keyProps.contains(prop)) {
                continue;
            }
            if (changedProps != null) {
                changedProps.add(prop);
            }
            updatedGetters.add(getter);
        }
        boolean fakeUpdate = false;
        if (updatedGetters.isEmpty() && discriminatorProp == null && nullGetters.isEmpty()) {
            if (hasOptimisticLock) {
                fakeUpdate = versionGetter == null;
            } else {
                if (!forceAllRows) {
                    fillIds(QueryReason.GET_ID_WHEN_UPDATE_NOTHING, originalKeyObjMap, batch);
                    return EMPTY_ROW_COUNTS;
                }
                fakeUpdate = true;
            }
        }
        if (keyProps != null && !sqlClient.getDialect().isIdFetchableByKeyUpdate()) {
            fillIds(QueryReason.GET_ID_FOR_KEY_BASE_UPDATE, originalKeyObjMap, batch);
            if (batch.entities().isEmpty()) {
                return EMPTY_ROW_COUNTS;
            }
        }
        MutationTrigger trigger = fireTrigger ? ctx.trigger : null;
        EntityCollection<DraftSpi> entities = changedProps != null ?
                new EntityList<>(batch.entities().size()) :
                null;
        // Drafts keep being mutated in place after this point (id/version filled in
        // below, and possibly reshaped later by `Saver.fetchImpl`/`replaceDraft` to
        // match an explicitly requested `Fetcher`). Recording trigger events must be
        // deferred until the draft reflects its final, post-execution state, so we
        // only remember which (oldRow, draft) pairs need an event here.
        List<Object[]> pendingTriggerData = trigger != null ? new ArrayList<>() : null;
        if (entities != null || trigger != null) {
            if (keyProps != null) {
                Map<Object, ImmutableSpi> subMap = originalIdObjMap != null ?
                        originalKeyObjMap.getOrDefault(group, Collections.emptyMap()) :
                        Collections.emptyMap();
                for (DraftSpi draft : batch.entities()) {
                    ImmutableSpi oldRow = subMap.get(Keys.keyOf(draft, keyProps));
                    if (isChanged(changedProps, oldRow, draft)) {
                        if (pendingTriggerData != null) {
                            pendingTriggerData.add(new Object[]{oldRow, draft});
                        }
                        if (entities != null) {
                            entities.add(draft);
                        }
                    }
                }
            } else {
                PropId idPropId = ctx.path.getType().getIdProp().getId();
                for (DraftSpi draft : batch.entities()) {
                    ImmutableSpi oldRow = originalIdObjMap != null ?
                            originalIdObjMap.get(draft.__get(idPropId)) :
                            null;
                    if (isChanged(changedProps, oldRow, draft) || hasOptimisticLock) {
                        if (pendingTriggerData != null) {
                            pendingTriggerData.add(new Object[]{oldRow, draft});
                        }
                        if (entities != null) {
                            entities.add(draft);
                        }
                    }
                }
            }
        }
        if (forceAllRows) {
            entities = batch.entities();
        } else if (entities == null) {
            entities = batch.entities();
        }
        SaveReturning returning = SaveReturning.forUpdate(
                ctx,
                shape,
                entities,
                updatedGetters,
                discriminatorProp,
                discriminatorGuardProp,
                discriminatorGuardValue,
                nullGetters,
                null,
                keyProps,
                userOptimisticLockPredicate,
                versionGetter,
                fakeUpdate,
                forceOneByOne
        );
        int[] rowCounts;
        if (returning != null) {
            rowCounts = returning.executeUpdate(entities);
        } else {
            BatchSqlBuilder builder = new BatchSqlBuilder(
                    sqlClient,
                    entities.size() < 2 || ctx.options.isBatchForbidden() || forceOneByOne
            );
            Dialect.UpdateContext updateContext = new UpdateContextImpl(
                    builder,
                    tableType,
                    shape,
                    Shape.fullOf(sqlClient, shape.getType().getJavaClass()).getIdGetters().get(0),
                    keyProps,
                    updatedGetters,
                    discriminatorProp,
                    discriminatorGuardProp,
                    discriminatorGuardValue,
                    nullGetters,
                    userOptimisticLockPredicate,
                    versionGetter,
                    fakeUpdate
            );
            sqlClient.getDialect().update(updateContext);
            rowCounts = executeAndGetRowCounts(
                    builder,
                    shape,
                    entities,
                    true,
                    false,
                    forceOneByOne
            );
        }
        if (versionGetter != null || userOptimisticLockPredicate != null) {
            int index = 0;
            for (DraftSpi row : entities) {
                if (rowCounts[index++] == 0) {
                    ctx.throwOptimisticLockError(row);
                }
            }
        }
        if (pendingTriggerData != null) {
            for (Object[] pair : pendingTriggerData) {
                trigger.modifyEntityTable(pair[0], pair[1]);
            }
        }
        AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount(rowCounts));
        return rowCounts;
    }

    private void fillIds(
            QueryReason queryReason,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch
    ) {
        int[] rowCounts = fillIdsAndGetRowCounts(queryReason, originalKeyObjMap, batch);
        int index = 0;
        for (Iterator<DraftSpi> itr = batch.entities().iterator(); itr.hasNext(); ) {
            itr.next();
            if (index >= rowCounts.length || rowCounts[index++] == 0) {
                itr.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private int[] fillIdsAndGetRowCounts(
            QueryReason queryReason,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch
    ) {
        KeyMatcher.Group group = batch.shape().group(
                ctx.options.getKeyMatcher(ctx.path.getType())
        );
        Set<ImmutableProp> keyProps = group.getProps();
        Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> keyMap = originalKeyObjMap;
        if (keyMap == null) {
            Fetcher<ImmutableSpi> fetcher = new FetcherImpl<>(
                    (Class<ImmutableSpi>) ctx.path.getType().getJavaClass()
            );
            for (ImmutableProp keyProp : keyProps) {
                if (keyProp.isReference(TargetLevel.ENTITY)) {
                    fetcher = fetcher.add(keyProp.getName(), IdOnlyFetchType.RAW);
                } else {
                    fetcher = fetcher.add(keyProp.getName());
                }
            }
            keyMap = Rows.findMapByKeys(
                    ctx,
                    queryReason,
                    fetcher,
                    batch.entities()
            );
        }
        Map<Object, ImmutableSpi> subMap = keyMap.getOrDefault(group, Collections.emptyMap());
        PropId idPropId = ctx.path.getType().getIdProp().getId();
        int[] rowCounts = new int[batch.entities().size()];
        int index = 0;
        for (DraftSpi draft : batch.entities()) {
            ImmutableSpi row = subMap.get(Keys.keyOf(draft, keyProps));
            if (row != null) {
                draft.__set(idPropId, row.__get(idPropId));
                rowCounts[index] = 1;
            }
            index++;
        }
        return rowCounts;
    }

    public MutationRows upsert(Batch<DraftSpi> batch, boolean ignoreUpdate) {
        if (batch.entities().isEmpty()) {
            return MutationRows.EMPTY;
        }
        ImmutableType type = batch.shape().getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        boolean typeChangeAllowed = ctx.options.isTypeChangeAllowed(type) && !ignoreUpdate;
        if (inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                (inheritanceInfo.getRootType() != type || typeChangeAllowed)) {
            return upsertJoined(batch, inheritanceInfo, ignoreUpdate);
        }
        upsert(
                batch,
                ctx.path.getType(),
                discriminatorProp(inheritanceInfo),
                typeChangeAllowed,
                typeChangeAllowed ? null : discriminatorGuardProp(inheritanceInfo, type),
                typeChangeAllowed ? redundantSingleTableGetters(inheritanceInfo, type) : Collections.emptyList(),
                ignoreUpdate,
                false
        );
        return MutationRows.UNKNOWN;
    }

    private MutationRows upsertJoined(Batch<DraftSpi> batch, InheritanceInfo inheritanceInfo, boolean ignoreUpdate) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        DraftSpi sample = batch.entities().iterator().next();
        ImmutableType rootType = inheritanceInfo.getRootType();
        Shape rootShape = joinedRootShape(sqlClient, rootType, sample);
        Batch<DraftSpi> rootBatch = batchOf(batch, rootShape);
        boolean typeChangeAllowed = ctx.options.isTypeChangeAllowed(batch.shape().getType()) && !ignoreUpdate;
        if (typeChangeAllowed && rootShape.getIdGetters().isEmpty()) {
            fillIdsAndGetRowCounts(
                    QueryReason.GET_ID_FOR_KEY_BASE_UPSERT,
                    null,
                    rootBatch
            );
        }
        Map<Object, ImmutableSpi> typeChangeOldRowMap =
                typeChangeAllowed && ctx.trigger != null ?
                        findTypeChangeOldRows(rootBatch, inheritanceInfo) :
                        Collections.emptyMap();
        TypeChangeRows typeChangeRows = typeChangeAllowed ?
                (
                        ctx.trigger != null ?
                                typeChangeRows(typeChangeOldRowMap.values()) :
                                resolveOldTypeForChange(rootBatch, inheritanceInfo)
                ) :
                null;
        boolean forceRootOneByOne =
                !typeChangeAllowed &&
                        sqlClient.getDialect().isBatchDumb();
        int[] rootRowCounts = EMPTY_ROW_COUNTS;
        Set<Object> acceptedTypeChangeIds = null;
        TypeChangeRows acceptedTypeChangeRows = typeChangeRows;
        Batch<DraftSpi> acceptanceBatch = rootBatch;
        if (typeChangeAllowed) {
            Set<Object> existingIds = typeChangeRows != null ?
                    typeChangeRows.ids() :
                    Collections.emptySet();
            acceptedTypeChangeIds = updateRootForTypeChange(
                    null,
                    null,
                    rootBatch,
                    rootType,
                    batch.shape().getType(),
                    inheritanceInfo,
                    typeChangeRows
            );
            Batch<DraftSpi> missingRootBatch = batchOfRowsNotIn(rootBatch, existingIds);
            if (!missingRootBatch.entities().isEmpty()) {
                if (ctx.trigger != null) {
                    insert(
                            missingRootBatch,
                            rootType,
                            discriminatorProp(inheritanceInfo),
                            true,
                            false
                    );
                    collectIds(acceptedTypeChangeIds, missingRootBatch);
                } else {
                    int[] insertedRowCounts = upsert(
                            missingRootBatch,
                            rootType,
                            discriminatorProp(inheritanceInfo),
                            false,
                            null,
                            Collections.emptyList(),
                            true,
                            false,
                            sqlClient.getDialect().isBatchDumb()
                    );
                    collectAcceptedIds(acceptedTypeChangeIds, missingRootBatch, insertedRowCounts);
                }
            }
            fireTypeChangeTriggers(batch, typeChangeOldRowMap, acceptedTypeChangeIds);
            acceptedTypeChangeRows = typeChangeRows != null ?
                    typeChangeRows.filteredBy(acceptedTypeChangeIds) :
                    null;
            rootBatch = batchOfRows(rootBatch, acceptedTypeChangeIds);
            batch = batchOfRows(batch, acceptedTypeChangeIds);
            if (batch.entities().isEmpty()) {
                return MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, acceptedTypeChangeIds));
            }
            sample = batch.entities().iterator().next();
        } else {
            rootRowCounts = upsert(
                    rootBatch,
                    rootType,
                    discriminatorProp(inheritanceInfo),
                    false,
                    discriminatorProp(inheritanceInfo),
                    Collections.emptyList(),
                    ignoreUpdate,
                    !ignoreUpdate,
                    forceRootOneByOne
            );
            if (forceRootOneByOne && !ignoreUpdate && rootShape.getIdGetters().isEmpty()) {
                rootRowCounts = fillIdsAndGetRowCounts(
                        QueryReason.GET_ID_FOR_KEY_BASE_UPSERT,
                        null,
                        rootBatch
                );
            }
        }
        int[] acceptedRowCounts = null;
        if (typeChangeAllowed) {
            deleteRedundantJoinedRows(batch, inheritanceInfo, acceptedTypeChangeRows);
        } else {
            acceptedRowCounts = rootRowCounts;
            acceptanceBatch = rootBatch;
            batch = batchOfChangedRows(batch, rootRowCounts);
            if (batch.entities().isEmpty()) {
                return MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, acceptedRowCounts));
            }
            sample = batch.entities().iterator().next();
        }
        ImmutableType previousTableType = rootType;
        for (ImmutableType tableType : joinedTableTypes(rootType, batch.shape().getType())) {
            Shape shape = joinedStageShape(sqlClient, previousTableType, tableType, sample);
            Batch<DraftSpi> childBatch = batchOf(batch, shape);
            if (typeChangeAllowed) {
                saveJoinedTableForTypeChange(
                        childBatch,
                        tableType,
                        rootType,
                        acceptedTypeChangeRows
                );
            } else if (ignoreUpdate) {
                insert(childBatch, tableType, null, true);
            } else {
                upsert(childBatch, tableType, null, false, null, Collections.emptyList(), false, false);
            }
            previousTableType = tableType;
        }
        return acceptedRowCounts != null ?
                MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, acceptedRowCounts)) :
                MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, acceptedTypeChangeIds));
    }

    private void updateJoinedChildWithRootGuard(
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            ImmutableType rootType,
            InheritanceInfo inheritanceInfo
    ) {
        List<PropertyGetter> updatedGetters = new ArrayList<>();
        for (PropertyGetter getter : batch.shape().getGetters()) {
            ImmutableProp prop = getter.prop();
            if (!prop.isId() && prop.isColumnDefinition()) {
                updatedGetters.add(getter);
            }
        }
        if (updatedGetters.isEmpty()) {
            return;
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        SaveReturning returning = SaveReturning.forUpdate(
                ctx,
                batch.shape(),
                batch.entities(),
                updatedGetters,
                null,
                null,
                null,
                Collections.emptyList(),
                SaveReturningUpdateCondition.joinedChildGuard(sqlClient, rootType, tableType, inheritanceInfo),
                null,
                null,
                null,
                false,
                false
        );
        if (returning != null) {
            int[] rowCounts = returning.executeUpdate(batch.entities());
            AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount(rowCounts));
            return;
        }
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        List<PropertyGetter> idGetters = Shape.fullOf(sqlClient, tableType.getJavaClass()).getIdGetters();
        PropertyGetter discriminatorGetter = discriminatorGetterForBatch(batch, inheritanceInfo);
        String childTableName = tableType.getTableName(strategy);

        BatchSqlBuilder builder = new BatchSqlBuilder(
                sqlClient,
                batch.entities().size() < 2 || ctx.options.isBatchForbidden()
        );
        builder.sql("update ")
                .sql(childTableName)
                .enter(BatchSqlBuilder.ScopeType.SET);
        for (PropertyGetter getter : updatedGetters) {
            builder.separator()
                    .sql(getter)
                    .sql(" = ")
                    .variable(getter);
        }
        builder.leave()
                .enter(BatchSqlBuilder.ScopeType.WHERE);
        for (PropertyGetter idGetter : idGetters) {
            builder.separator()
                    .sql(idGetter)
                    .sql(" = ")
                    .variable(idGetter);
        }
        builder.separator();
        InheritanceMutationUtils.renderJoinedChildRootGuard(
                builder,
                sqlClient,
                rootType,
                inheritanceInfo,
                idGetters,
                discriminatorGetter
        );
        builder.leave();
        int rowCount = execute(builder, batch, true, false);
        AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount);
    }

    private PropertyGetter discriminatorGetterForBatch(Batch<DraftSpi> batch, InheritanceInfo inheritanceInfo) {
        return PropertyGetter
                .propertyGetters(
                        ctx.options.getSqlClient(),
                        inheritanceInfo.getDiscriminatorProp(batch.shape().getType())
                )
                .get(0);
    }

    private int[] upsert(
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            boolean updateDiscriminator,
            @Nullable ImmutableProp discriminatorGuardProp,
            List<PropertyGetter> nullGetters,
            boolean ignoreUpdate,
            boolean forceMatchedUpdate
    ) {
        return upsert(
                batch,
                tableType,
                discriminatorProp,
                updateDiscriminator,
                discriminatorGuardProp,
                nullGetters,
                ignoreUpdate,
                forceMatchedUpdate,
                false
        );
    }

    private int[] upsert(
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            boolean updateDiscriminator,
            @Nullable ImmutableProp discriminatorGuardProp,
            List<PropertyGetter> nullGetters,
            boolean ignoreUpdate,
            boolean forceMatchedUpdate,
            boolean forceOneByOne
    ) {

        Collection<ImmutableProp> implicitKeyProps = implicitKeyProps(discriminatorGuardProp);
        validate(batch.shape(), false, implicitKeyProps);
        if (batch.entities().isEmpty()) {
            return EMPTY_ROW_COUNTS;
        }
        if (!forceMatchedUpdate &&
                ctx.options.isIdOnlyAsReference(ctx.path.getProp()) &&
                batch.shape().isIdOnly()) {
            return EMPTY_ROW_COUNTS;
        }

        if (ctx.trigger != null) {
            throw new AssertionError(
                    "Internal bug: " +
                            "Upsert cannot be called if the trigger is not null"
            );
        }

        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Shape fullShape = Shape.fullOf(sqlClient, tableType.getJavaClass());
        List<PropertyGetter> defaultGetters = new ArrayList<>();
        for (PropertyGetter getter : fullShape.getColumnDefinitionGetters()) {
            if (getter.metadata().hasDefaultValue() && !batch.shape().contains(getter)) {
                defaultGetters.add(getter);
            }
        }
        SequenceIdGenerator sequenceIdGenerator = null;
        if (batch.shape().getIdGetters().isEmpty()) {
            IdGenerator idGenerator = sqlClient.getIdGenerator(tableType.getJavaClass());
            if (idGenerator instanceof SequenceIdGenerator) {
                sequenceIdGenerator = (SequenceIdGenerator) idGenerator;
            } else if (!(idGenerator instanceof IdentityIdGenerator)) {
                ctx.throwIllegalIdGenerator(
                        "In order to upsert object without id, " +
                                "the id generator must be IdentityGenerator or Sequence"
                );
            }
        }

        List<ImmutableProp> conflictProps;
        List<PropertyGetter> conflictGetters;
        LogicalDeletedInfo conflictPredicate;
        if (!batch.shape().getIdGetters().isEmpty()) {
            conflictProps = Collections.singletonList(batch.shape().getType().getIdProp());
            conflictGetters = batch.shape().getIdGetters();
            conflictPredicate = null;
        } else {
            Set<ImmutableProp> keyProps = batch.shape().keyProps(
                    ctx.options.getKeyMatcher(tableType),
                    implicitKeyProps
            );
            LogicalDeletedInfo logicalDeletedInfo = batch.shape().getType().getLogicalDeletedInfo();
            boolean filteredLogicalDeletedKey = isFilteredLogicalDeletedKey(batch.shape().getType());
            conflictProps = MutationKeys.keyAndLogicalDeletedProps(batch.shape().getType(), keyProps);
            conflictGetters = new ArrayList<>();
            for (PropertyGetter getter : fullShape.getGetters()) {
                if (keyProps.contains(getter.prop())) {
                    conflictGetters.add(getter);
                } else if (!filteredLogicalDeletedKey && getter.prop().isLogicalDeleted()) {
                    conflictGetters.add(getter);
                }
            }
            conflictPredicate = filteredLogicalDeletedKey ? logicalDeletedInfo : null;
        }
        UpsertMask<?> upsertMask = ctx.options.getUpsertMask(tableType);
        List<PropertyGetter> insertedGetters = new ArrayList<>();
        for (PropertyGetter getter : batch.shape().getColumnDefinitionGetters()) {
            if (getter.isInsertable(conflictProps, upsertMask)) {
                insertedGetters.add(getter);
            }
        }
        for (PropertyGetter getter : defaultGetters) {
            if (getter.isInsertable(conflictProps, upsertMask)) {
                insertedGetters.add(getter);
            }
        }

        List<PropertyGetter> updatedGetters = new ArrayList<>();
        if (!ignoreUpdate) {
            for (PropertyGetter getter : batch.shape().getGetters()) {
                if (getter.isUpdatable(conflictProps, upsertMask)) {
                    updatedGetters.add(getter);
                }
            }
        }

        Predicate userOptimisticLockPredicate = userLockOptimisticPredicate();
        PropertyGetter versionGetter = batch.shape().getVersionGetter();

        SaveReturning returning = SaveReturning.forUpsert(
                ctx,
                batch,
                tableType,
                batch.shape().getIdGetters().isEmpty() ? batch.shape().getType().getIdProp() : null,
                sequenceIdGenerator,
                insertedGetters,
                discriminatorProp,
                updateDiscriminator,
                discriminatorGuardProp,
                nullGetters,
                conflictGetters,
                conflictPredicate,
                updatedGetters,
                ignoreUpdate,
                userOptimisticLockPredicate,
                versionGetter,
                forceMatchedUpdate,
                forceOneByOne
        );
        if (returning != null) {
            int[] rowCounts = returning.executeUpsert(batch.entities());
            AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount(rowCounts));
            return rowCounts;
        }

        BatchSqlBuilder builder = new BatchSqlBuilder(
                sqlClient,
                batch.entities().size() < 2 || ctx.options.isBatchForbidden() || forceOneByOne
        );
        UpsertContextImpl upsertContext = new UpsertContextImpl(
                builder,
                tableType,
                batch.shape().getIdGetters().isEmpty() ? batch.shape().getType().getIdProp() : null,
                sequenceIdGenerator,
                insertedGetters,
                discriminatorProp,
                updateDiscriminator,
                discriminatorGuardProp,
                nullGetters,
                conflictGetters,
                conflictPredicate,
                updatedGetters,
                ignoreUpdate,
                userOptimisticLockPredicate,
                versionGetter,
                forceMatchedUpdate
        );
        sqlClient.getDialect().upsert(upsertContext);
        int[] rowCounts = executeAndGetRowCounts(
                builder,
                batch.shape(),
                batch.entities(),
                true,
                ignoreUpdate,
                forceOneByOne
        );
        AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount(rowCounts));
        return rowCounts;
    }

    private List<PropertyGetter> redundantSingleTableGetters(
            @Nullable InheritanceInfo inheritanceInfo,
            ImmutableType targetType
    ) {
        if (inheritanceInfo == null || inheritanceInfo.getStrategy() != InheritanceType.SINGLE_TABLE) {
            return Collections.emptyList();
        }
        ImmutableType rootType = inheritanceInfo.getRootType();
        List<PropertyGetter> redundantGetters = new ArrayList<>();
        collectRedundantSingleTableGetters(redundantGetters, rootType, targetType);
        for (ImmutableType derivedType : rootType.getAllDerivedTypes()) {
            collectRedundantSingleTableGetters(redundantGetters, derivedType, targetType);
        }
        return redundantGetters;
    }

    private void collectRedundantSingleTableGetters(
            List<PropertyGetter> getters,
            ImmutableType declaringType,
            ImmutableType targetType
    ) {
        if (declaringType.isAssignableFrom(targetType)) {
            return;
        }
        for (ImmutableProp prop : declaringType.getDeclaredProps().values()) {
            if (prop.isId() || prop.isVersion() || !prop.isColumnDefinition() || prop.isDiscriminator()) {
                continue;
            }
            getters.addAll(PropertyGetter.propertyGetters(ctx.options.getSqlClient(), prop));
        }
    }

    private void deleteRedundantJoinedRows(
            Batch<DraftSpi> batch,
            InheritanceInfo inheritanceInfo,
            @Nullable TypeChangeRows typeChangeRows
    ) {
        if (typeChangeRows == null || typeChangeRows.oldTypeIdMap.isEmpty()) {
            return;
        }
        ImmutableType rootType = inheritanceInfo.getRootType();
        ImmutableType targetType = batch.shape().getType();
        Set<ImmutableType> retainedTypes = new HashSet<>(joinedTableTypes(rootType, targetType));
        MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
        Map<ImmutableType, Set<Object>> tableIdMap = new LinkedHashMap<>();
        for (Map.Entry<ImmutableType, Set<Object>> e : typeChangeRows.oldTypeIdMap.entrySet()) {
            for (ImmutableType tableType : joinedTableTypes(rootType, e.getKey())) {
                if (!retainedTypes.contains(tableType)) {
                    tableIdMap
                            .computeIfAbsent(tableType, it -> new LinkedHashSet<>())
                            .addAll(e.getValue());
                }
            }
        }
        if (tableIdMap.isEmpty()) {
            return;
        }
        List<ImmutableType> tableTypes = new ArrayList<>(tableIdMap.keySet());
        tableTypes.sort((a, b) -> compareJoinedCleanupTableTypes(strategy, a, b));
        for (ImmutableType tableType : tableTypes) {
            Set<Object> ids = tableIdMap.get(tableType);
            BatchSqlBuilder builder = new BatchSqlBuilder(
                    ctx.options.getSqlClient(),
                    ids.size() < 2 || ctx.options.isBatchForbidden()
            );
            builder.sql("delete from ")
                    .sql(tableType.getTableName(strategy))
                    .sql(" where ")
                    .sql(rootType.getIdProp().<SingleColumn>getStorage(strategy).getName())
                    .sql(" = ")
                    .variable(id -> id);
            executeIdBatch(builder, ids);
        }
    }

    private boolean isFilteredLogicalDeletedKey(ImmutableType type) {
        LogicalDeletedInfo logicalDeletedInfo = type.getLogicalDeletedInfo();
        return logicalDeletedInfo != null && logicalDeletedInfo.getType() == boolean.class;
    }

    private void validate(Shape shape, boolean insertOnly) {
        validate(shape, insertOnly, Collections.emptySet());
    }

    private void validate(Shape shape, boolean insertOnly, Collection<ImmutableProp> implicitKeyProps) {
        Set<ImmutableProp> keyProps = shape.keyProps(
                ctx.options.getKeyMatcher(shape.getType()),
                implicitKeyProps
        );
        if (!insertOnly) {
            if (shape.isWild(keyProps)) {
                ctx.throwNeitherIdNorKey(shape.getType(), keyProps);
            }
        }
        MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
        if (!shape.getIdGetters().isEmpty()) {
            ImmutableProp idProp = shape.getType().getIdProp();
            ColumnDefinition definition = shape.getType().getIdProp().getStorage(strategy);
            if (shape.getIdGetters().size() < definition.size()) {
                ctx.throwIncompleteProperty(idProp, "id");
            }
        }
        Map<ImmutableProp, List<PropertyGetter>> getterMap = shape.getGetterMap();
        for (Map.Entry<ImmutableProp, List<PropertyGetter>> e : getterMap.entrySet()) {
            ImmutableProp prop = e.getKey();
            List<PropertyGetter> getter = e.getValue();
            if (prop.isReference(TargetLevel.ENTITY)) {
                ColumnDefinition definition = prop.getStorage(strategy);
                if (getter.size() < definition.size()) {
                    ctx.throwIncompleteProperty(prop, "associated id");
                }
            } else if (keyProps.contains(prop)) {
                ColumnDefinition definition = prop.getStorage(strategy);
                if (getter.size() < definition.size()) {
                    ctx.throwIncompleteProperty(prop, "key");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Predicate userLockOptimisticPredicate() {

        UserOptimisticLock<Object, Table<Object>> userOptimisticLock =
                (UserOptimisticLock<Object, Table<Object>>) ctx.options.getUserOptimisticLock(ctx.path.getType());
        if (userOptimisticLock == null) {
            return null;
        }
        MutableRootQueryImpl<?> fakeQuery = new MutableRootQueryImpl<>(
                ctx.options.getSqlClient(),
                ctx.path.getType(),
                ExecutionPurpose.MUTATE,
                FilterLevel.DEFAULT
        );
        Table<?> table = fakeQuery.getTable();
        if (table instanceof TableImplementor<?>) {
            table = new UntypedJoinDisabledTableProxy<>(
                    (TableImplementor<?>) table,
                    GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON
            );
        } else {
            table = ((TableProxy<?>) table).__disableJoin(GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON);
        }
        Predicate predicate = userOptimisticLock.predicate(
                (Table<Object>) table,
                OptimisticLockValueFactoryFactories.<Object>of()
        );
        validateUserOptimisticLockPredicate(predicate);
        return predicate;
    }

    private void validateUserOptimisticLockPredicate(Predicate predicate) {
        InheritanceInfo inheritanceInfo = ctx.path.getType().getInheritanceInfo();
        if (inheritanceInfo == null || inheritanceInfo.getStrategy() != InheritanceType.JOINED) {
            return;
        }
        ImmutableType rootType = inheritanceInfo.getRootType();
        ((Ast) predicate).accept(new AstVisitor(new AstContext(ctx.options.getSqlClient())) {

            @Override
            public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
                validateRootOptimisticLockProp(rootType, prop);
            }

            @Override
            public void visitOptimisticLockNewValue(ImmutableProp prop) {
                validateRootOptimisticLockProp(rootType, prop);
            }
        });
    }

    private static void validateRootOptimisticLockProp(ImmutableType rootType, @Nullable ImmutableProp prop) {
        if (prop == null) {
            return;
        }
        ImmutableProp originalProp = prop.toOriginal();
        if (!originalProp.getDeclaringType().isAssignableFrom(rootType)) {
            throw new IllegalArgumentException(
                    "User optimistic lock predicate for joined inheritance type \"" +
                            rootType +
                            "\" can only reference root-table properties, but \"" +
                            prop +
                            "\" is not declared by the inheritance root or its mapped superclasses"
            );
        }
    }

    private boolean isChanged(Set<ImmutableProp> props, ImmutableSpi oldRow, ImmutableSpi newRow) {
        if (oldRow == null) {
            return true;
        }
        boolean changed = false;
        for (ImmutableProp prop : props) {
            PropId propId = prop.getId();
            boolean isFrozenBackReference = ctx.backReferenceFrozen && prop == ctx.backReferenceProp;
            if (!oldRow.__isLoaded(propId)) {
                if (isFrozenBackReference) {
                    ctx.throwUnloadedFrozenBackReference(ctx.backReferenceProp);
                }
                changed = true;
            } else {
                Object oldValue = oldRow.__get(propId);
                Object newValue = newRow.__get(propId);
                if (isFrozenBackReference && !Objects.equals(oldValue, newValue)) {
                    ctx.throwTargetIsNotTransferable(newRow);
                } else if (!changed && !Objects.equals(oldValue, newValue)) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    private int[] executeAndGetRowCounts(
            BatchSqlBuilder builder,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            boolean updatable,
            boolean ignoreUpdate
    ) {
        return executeAndGetRowCounts(builder, shape, entities, updatable, ignoreUpdate, false);
    }

    private int[] executeAndGetRowCounts(
            BatchSqlBuilder builder,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            boolean updatable,
            boolean ignoreUpdate,
            boolean forceOneByOne
    ) {
        if (entities.isEmpty()) {
            return EMPTY_ROW_COUNTS;
        }
        if (forceOneByOne || entities.size() < 2 || ctx.options.isBatchForbidden() || isForcedOneByOne(shape, entities)) {
            return executeAndGetRowCountsOneByOne(builder, shape, entities, updatable, ignoreUpdate);
        }
        return executeAndGetRowCountsByBatch(builder, shape, entities, updatable, ignoreUpdate);
    }

    private boolean isForcedOneByOne(
            Shape shape,
            EntityCollection<DraftSpi> entities
    ) {
        if (!ctx.options.getSqlClient().getDialect().isBatchDumb()) {
            return false;
        }
        ImmutableProp deepestProp = ctx.path.getProp();
        if (deepestProp != null) {
            if (ctx.options.getAssociatedMode(deepestProp) == AssociatedSaveMode.REPLACE) {
                return true;
            }
            if (deepestProp.isColumnDefinition()) {
                return true;
            }
        }
        List<PropId> postAssociationPropIds = new ArrayList<>();
        for (ImmutableProp prop : shape.getType().getProps().values()) {
            if (!prop.isColumnDefinition() && prop.isAssociation(TargetLevel.ENTITY)) {
                postAssociationPropIds.add(prop.getId());
            }
        }
        if (postAssociationPropIds.isEmpty()) {
            return false;
        }
        PropId idPropId = shape.getType().getIdProp().getId();
        Iterator<EntityCollection.Item<DraftSpi>> itr = entities.items().iterator();
        while (itr.hasNext()) {
            EntityCollection.Item<DraftSpi> item = itr.next();
            if (item.getEntity().__isLoaded(idPropId)) {
                continue;
            }
            for (DraftSpi originalEntity : item.getOriginalEntities()) {
                for (PropId postAssociationPropId : postAssociationPropIds) {
                    if (originalEntity.__isLoaded(postAssociationPropId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int[] executeAndGetRowCountsOneByOne(
            BatchSqlBuilder builder,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            boolean updatable,
            boolean ignoreUpdate
    ) {
        JSqlClientImplementor sqlClient = builder.sqlClient();
        Tuple3<String, BatchSqlBuilder.VariableMapper, List<Integer>> tuple = builder.build();
        Executor executor = sqlClient.getExecutor();
        String sql = tuple.get_1();
        BatchSqlBuilder.VariableMapper mapper = tuple.get_2();
        int[] rowCounts = new int[entities.size()];
        int rowIndex = 0;
        ImmutableProp autoIdProp = shape.getIdGetters().isEmpty() ? shape.getType().getIdProp() : null;
        Reader<?> autoIdReader = autoIdProp != null ? sqlClient.getReader(autoIdProp) : null;

        for (EntityCollection.Item<DraftSpi> item : entities.items()) {
            List<Object> variables = mapper.variables(item.getEntity());
            rowCounts[rowIndex++] = executor.execute(
                    new Executor.Args<>(
                            sqlClient,
                            ctx.con,
                            sql,
                            variables,
                            tuple.get_3(),
                            ExecutionPurpose.MUTATE,
                            ctx.options.getExceptionTranslator(),
                            (con, sqlText) -> {
                                if (shape.getIdGetters().isEmpty()) {
                                    return con.prepareStatement(sqlText, Statement.RETURN_GENERATED_KEYS);
                                }
                                return con.prepareStatement(sqlText);
                            },
                            (stmt, args) -> {
                                int rowCount;
                                try {
                                    Savepoint savepoint = ctx.options.isConstraintViolationTranslatable() ?
                                            SavepointManager.setIfNeeded(ctx.con, sqlClient) :
                                            null;
                                    try {
                                        rowCount = stmt.executeUpdate();
                                    } catch (SQLException ex) {
                                        SavepointManager.rollback(stmt::getConnection, savepoint);
                                        throw ex;
                                    } finally {
                                        SavepointManager.release(stmt::getConnection, savepoint);
                                    }
                                } catch (SQLException ex) {
                                    Exception translateException = translateException(ex, args, shape, item.getEntity(), updatable);
                                    if (translateException instanceof RuntimeException) {
                                        throw (RuntimeException) translateException;
                                    }
                                    throw new ExecutionException("Cannot execute the DML statement", translateException);
                                }
                                Object id = null;
                                if (autoIdReader != null) {
                                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                                        if (rs.next()) {
                                            id = autoIdReader.read(rs, new Reader.Context(null, sqlClient));
                                        }
                                    }
                                }
                                modifyEntity(
                                        id,
                                        shape,
                                        item,
                                        updatable,
                                        ignoreUpdate,
                                        rowCount
                                );
                                return rowCount;
                            }
                    )
            );
        }
        return rowCounts;
    }

    private int[] executeAndGetRowCountsByBatch(
            BatchSqlBuilder builder,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            boolean updatable,
            boolean ignoreUpdate
    ) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Tuple3<String, BatchSqlBuilder.VariableMapper, ?> tuple = builder.build();
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor()
                .executeBatch(
                        ctx.con,
                        tuple.get_1(),
                        shape.getIdGetters().isEmpty() ? ctx.path.getType().getIdProp() : null,
                        ExecutionPurpose.command(QueryReason.NONE),
                        sqlClient,
                        ctx.options.isConstraintViolationTranslatable()
                )
        ) {
            BatchSqlBuilder.VariableMapper mapper = tuple.get_2();
            for (DraftSpi draft : entities) {
                batchContext.add(mapper.variables(draft));
            }
            int[] rowCounts = batchContext.execute((ex, args) -> {
                Executor.BatchContext ctx = (Executor.BatchContext) args;
                if (ex instanceof BatchUpdateException) {
                    modifyEntities(
                            ctx.generatedIds(),
                            shape,
                            entities,
                            updatable,
                            ignoreUpdate,
                            ((BatchUpdateException) ex).getUpdateCounts()
                    );
                }
                return translateException(ex, ctx, shape, entities, updatable);
            });
            if (sqlClient.getDialect().isBatchDumb()) {
                modifyEntities(null, shape, entities, updatable, ignoreUpdate, rowCounts);
            } else {
                modifyEntities(batchContext.generatedIds(), shape, entities, updatable, ignoreUpdate, rowCounts);
            }
            return rowCounts;
        }
    }

    private void modifyEntity(
            Object generatedId,
            Shape shape,
            EntityCollection.Item<DraftSpi> item,
            boolean updatable,
            boolean ignoreUpdate,
            int rowCount
    ) {
        if (generatedId != null) {
            PropId idPropId = ctx.path.getType().getIdProp().getId();
            for (DraftSpi draft : item.getOriginalEntities()) {
                draft.__set(idPropId, generatedId);
            }
        }

        PropertyGetter versionGetter = shape.getVersionGetter();
        if (updatable && versionGetter != null) {
            PropId versionPropId = versionGetter.prop().getId();
            Integer version = (Integer) item.getEntity().__get(versionPropId);
            if (rowCount > 0) {
                for (DraftSpi draft : item.getOriginalEntities()) {
                    draft.__set(versionPropId, version + 1);
                }
            }
        } else if (ignoreUpdate) {
            List<PropId> unloadedPropIds = new ArrayList<>();
            for (ImmutableProp prop : ctx.path.getType().getProps().values()) {
                if (!prop.isMiddleTableDefinition() && prop.isAssociation(TargetLevel.PERSISTENT)) {
                    unloadedPropIds.add(prop.getId());
                }
            }
            if (unloadedPropIds.isEmpty()) {
                return;
            }
            if (rowCount <= 0) {
                for (PropId unloadedPropId : unloadedPropIds) {
                    for (DraftSpi draft : item.getOriginalEntities()) {
                        draft.__unload(unloadedPropId);
                    }
                }
            }
        }
    }

    private void modifyEntities(
            Object[] generatedIds,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            boolean updatable,
            boolean ignoreUpdate,
            int[] rowCounts
    ) {
        int generateIdIndex = 0;
        int rowIndex = 0;
        for (EntityCollection.Item<DraftSpi> item : entities.items()) {
            modifyEntity(
                    generatedIds != null && generatedIds.length != 0 && rowCounts[rowIndex] > 0 ?
                            generatedIds[generateIdIndex++] :
                            null,
                    shape,
                    item,
                    updatable,
                    ignoreUpdate,
                    rowIndex < rowCounts.length ?
                            rowCounts[rowIndex] :
                            -1
            );
            rowIndex++;
        }
    }

    private int execute(
            BatchSqlBuilder builder,
            Batch<DraftSpi> batch,
            boolean updatable,
            boolean ignoreUpdate
    ) {
        int[] rowCounts = executeAndGetRowCounts(
                builder,
                batch.shape(),
                batch.entities(),
                updatable,
                ignoreUpdate
        );
        return rowCount(rowCounts);
    }

    private int executeIdBatch(BatchSqlBuilder builder, Collection<Object> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        JSqlClientImplementor sqlClient = builder.sqlClient();
        Tuple3<String, BatchSqlBuilder.VariableMapper, List<Integer>> tuple = builder.build();
        String sql = tuple.get_1();
        BatchSqlBuilder.VariableMapper mapper = tuple.get_2();
        if (ids.size() < 2 || ctx.options.isBatchForbidden()) {
            int rowCount = 0;
            for (Object id : ids) {
                rowCount += sqlClient.getExecutor().execute(
                        new Executor.Args<Integer>(
                                sqlClient,
                                ctx.con,
                                sql,
                                mapper.variables(id),
                                tuple.get_3(),
                                ExecutionPurpose.MUTATE,
                                ctx.options.getExceptionTranslator(),
                                null,
                                (stmt, args) -> stmt.executeUpdate()
                        )
                );
            }
            return rowCount;
        }
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor()
                .executeBatch(
                        ctx.con,
                        sql,
                        null,
                        ExecutionPurpose.command(QueryReason.NONE),
                        sqlClient,
                        ctx.options.isConstraintViolationTranslatable()
                )
        ) {
            for (Object id : ids) {
                batchContext.add(mapper.variables(id));
            }
            return rowCount(batchContext.execute((ex, args) -> convertFinalException(ex, args)));
        }
    }

    private Exception translateException(
            SQLException ex,
            Executor.Args<?> args,
            Shape shape,
            DraftSpi entity,
            boolean updatable
    ) {
        String state = ex.getSQLState();
        if (state == null || !state.startsWith("23")) {
            return convertFinalException(ex, args);
        }
        EntityInvestigator investigator = new EntityInvestigator(
                SIMPLE_ILLEGAL_ROW_COUNTS,
                this.ctx.investigator(ctx.options.getSqlClient()),
                shape,
                Collections.singletonList(entity),
                updatable
        );
        Exception investigateEx = investigator.investigate();
        if (investigateEx == null) {
            investigateEx = ex;
        }
        return convertFinalException(investigateEx, args);
    }

    private Exception translateException(
            SQLException ex,
            @Nullable Executor.BatchContext ctx,
            Shape shape,
            Collection<? extends DraftSpi> entities,
            boolean updatable
    ) {
        String state = ex.getSQLState();
        if (state == null || !state.startsWith("23") || !(ex instanceof BatchUpdateException)) {
            return convertFinalException(ex, ctx);
        }
        BatchUpdateException bue = (BatchUpdateException) ex;
        EntityInvestigator investigator = new EntityInvestigator(
                bue.getUpdateCounts(),
                this.ctx.investigator(ctx),
                shape,
                entities,
                updatable
        );
        Exception investigateEx = investigator.investigate();
        if (investigateEx == null) {
            investigateEx = bue;
        }
        return convertFinalException(investigateEx, ctx);
    }

    private Exception convertFinalException(@NotNull Exception ex, @NotNull ExceptionTranslator.Args args) {
        ExceptionTranslator<Exception> translator =
                this.ctx.options.getExceptionTranslator();
        if (translator == null) {
            return ex;
        }
        return translator.translate(ex, args);
    }

    private class UpdateContextImpl implements Dialect.UpdateContext {

        private final BatchSqlBuilder builder;

        private final ImmutableType tableType;

        private final Shape shape;

        private final PropertyGetter idGetter;

        private final Set<ImmutableProp> keyProps;

        private final List<PropertyGetter> updatedGetters;

        @Nullable
        private final PropertyGetter discriminatorGetter;

        @Nullable
        private final PropertyGetter discriminatorGuardGetter;

        @Nullable
        private final Object discriminatorGuardValue;

        private final List<PropertyGetter> nullGetters;

        private final Predicate userOptimisticLockPredicate;

        private final PropertyGetter versionGetter;

        private final boolean fakeUpdate;

        UpdateContextImpl(
                BatchSqlBuilder builder,
                ImmutableType tableType,
                Shape shape,
                PropertyGetter idGetter,
                Set<ImmutableProp> keyProps,
                List<PropertyGetter> updatedGetters,
                @Nullable ImmutableProp discriminatorProp,
                @Nullable ImmutableProp discriminatorGuardProp,
                @Nullable Object discriminatorGuardValue,
                List<PropertyGetter> nullGetters,
                Predicate userOptimisticLockPredicate,
                PropertyGetter versionGetter,
                boolean fakeUpdate
        ) {
            this.builder = builder;
            this.tableType = tableType;
            this.shape = shape;
            this.idGetter = idGetter;
            this.keyProps = keyProps;
            this.updatedGetters = updatedGetters;
            this.discriminatorGetter = discriminatorProp != null ?
                    PropertyGetter.propertyGetters(ctx.options.getSqlClient(), discriminatorProp).get(0) :
                    null;
            if (discriminatorGuardProp != null) {
                ImmutableProp prop = shape
                        .getType()
                        .getInheritanceInfo()
                        .getDiscriminatorProp(shape.getType());
                this.discriminatorGuardGetter = PropertyGetter
                        .propertyGetters(ctx.options.getSqlClient(), prop)
                        .get(0);
            } else {
                this.discriminatorGuardGetter = null;
            }
            this.discriminatorGuardValue = discriminatorGuardValue;
            this.nullGetters = nullGetters;
            this.userOptimisticLockPredicate = userOptimisticLockPredicate;
            this.versionGetter = versionGetter;
            this.fakeUpdate = fakeUpdate;
        }

        @Override
        public boolean isIdInteger() {
            return Classes.INT_TYPES.contains(
                    shape.getType().getIdProp().getReturnClass()
            );
        }

        @Override
        public boolean isUpdatedByKey() {
            return shape.getIdGetters().isEmpty();
        }

        @Override
        public boolean hasUpdatedColumns() {
            return discriminatorGetter != null || !nullGetters.isEmpty() || !updatedGetters.isEmpty();
        }

        @Override
        public boolean isFakeUpdateRequired() {
            return fakeUpdate;
        }

        @Override
        public Dialect.UpdateContext sql(String sql) {
            builder.sql(sql);
            return this;
        }

        @Override
        public Dialect.UpdateContext sql(ValueGetter getter) {
            builder.sql(getter);
            return this;
        }

        @Override
        public Dialect.UpdateContext enter(AbstractSqlBuilder.ScopeType type) {
            builder.enter(type);
            return this;
        }

        @Override
        public Dialect.UpdateContext separator() {
            builder.separator();
            return this;
        }

        @Override
        public Dialect.UpdateContext leave() {
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpdateContext appendTableName() {
            MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
            builder.sql(tableType.getTableName(strategy));
            return this;
        }

        @Override
        public Dialect.UpdateContext appendAssignments() {
            boolean hasAssignment = false;
            if (discriminatorGetter != null) {
                builder.separator()
                        .sql(discriminatorGetter)
                        .sql(" = ")
                        .variable(discriminatorGetter);
                hasAssignment = true;
            }
            for (PropertyGetter getter : nullGetters) {
                builder.separator()
                        .sql(getter)
                        .sql(" = null");
                hasAssignment = true;
            }
            for (PropertyGetter getter : updatedGetters) {
                if (getter != versionGetter) {
                    builder.separator()
                            .sql(getter)
                            .sql(" = ")
                            .variable(getter);
                    hasAssignment = true;
                }
            }

            PropertyGetter actualVersionGetter = versionGetter;
            if (actualVersionGetter == null) {
                ImmutableProp versionProp = ctx.path.getType().getVersionProp();
                if (versionProp != null &&
                        ctx.options.getUnloadedVersionBehavior(ctx.path.getType()) == UnloadedVersionBehavior.INCREASE
                ) {
                    actualVersionGetter = PropertyGetter
                            .propertyGetters(ctx.options.getSqlClient(), versionProp)
                            .get(0);
                }
            }
            if (actualVersionGetter != null) {
                builder.separator()
                        .sql(actualVersionGetter)
                        .sql(" = ")
                        .sql(actualVersionGetter)
                        .sql(" + 1");
                hasAssignment = true;
            }
            if (!hasAssignment && fakeUpdate) {
                builder.separator()
                        .sql(Dialect.FAKE_UPDATE_COMMENT)
                        .sql(" ")
                        .sql(idGetter)
                        .sql(" = ")
                        .sql(idGetter);
            }
            return this;
        }

        @Override
        public Dialect.UpdateContext appendPredicates() {
            if (keyProps != null) {
                Map<ImmutableProp, List<PropertyGetter>> getterMap = shape.getGetterMap();
                for (ImmutableProp keyProp : keyProps) {
                    List<PropertyGetter> getters = getterMap.get(keyProp);
                    if (getters == null) {
                        if (keyProp.isDiscriminator()) {
                            getters = PropertyGetter.propertyGetters(ctx.options.getSqlClient(), keyProp);
                        }
                    }
                    if (getters == null) {
                        getters = PropertyGetter.propertyGetters(ctx.options.getSqlClient(), keyProp);
                    }
                    for (PropertyGetter getter : getters) {
                        builder.separator()
                                .sql(getter)
                                .sql(" = ")
                                .variable(getter);
                    }
                }
                LogicalDeletedInfo logicalDeletedInfo = shape.getType().getLogicalDeletedInfo();
                if (logicalDeletedInfo != null) {
                    builder.separator().logicalDeleteFilter(logicalDeletedInfo, null);
                }
            } else {
                for (PropertyGetter getter : shape.getIdGetters()) {
                    builder.separator()
                            .sql(getter)
                            .sql(" = ")
                            .variable(getter);
                }
            }
            if (versionGetter != null) {
                builder.separator()
                        .sql(versionGetter)
                        .sql(" = ")
                        .variable(versionGetter);
            }
            if (userOptimisticLockPredicate != null) {
                builder.separator();
                AbstractExpression.renderChild(
                        (Ast) userOptimisticLockPredicate,
                        ExpressionPrecedences.AND,
                        builder
                );
            }
            if (discriminatorGuardGetter != null &&
                    (keyProps == null || !keyProps.contains(discriminatorGuardGetter.prop()))) {
                builder.separator()
                        .sql(discriminatorGuardGetter)
                        .sql(" = ");
                if (discriminatorGuardValue != null) {
                    builder.variable(row -> discriminatorGuardValue);
                } else {
                    builder.variable(discriminatorGuardGetter);
                }
            }
            return this;
        }

        @Override
        public Dialect.UpdateContext appendId() {
            builder.sql(idGetter);
            return this;
        }
    }

    private class UpsertContextImpl implements Dialect.UpsertContext {

        private final BatchSqlBuilder builder;

        private final ImmutableType tableType;

        private final SequenceIdGenerator sequenceIdGenerator;

        private final PropertyGetter generatedIdGetter;

        private final List<PropertyGetter> insertedGetters;

        @Nullable
        private final PropertyGetter discriminatorGetter;

        private final boolean updateDiscriminator;

        @Nullable
        private final PropertyGetter discriminatorGuardGetter;

        private final List<PropertyGetter> nullGetters;

        private final List<PropertyGetter> conflictGetters;

        private final LogicalDeletedInfo conflictPredicate;

        private final List<PropertyGetter> updatedGetters;

        private final boolean updateIgnored;

        private final boolean fakeUpdate;

        private final Predicate userOptimisticLockPredicate;

        private final PropertyGetter versionGetter;

        private Boolean complete;

        UpsertContextImpl(
                BatchSqlBuilder builder,
                ImmutableType tableType,
                ImmutableProp generatedIdProp,
                SequenceIdGenerator sequenceIdGenerator,
                List<PropertyGetter> insertedGetters,
                @Nullable ImmutableProp discriminatorProp,
                boolean updateDiscriminator,
                @Nullable ImmutableProp discriminatorGuardProp,
                List<PropertyGetter> nullGetters,
                List<PropertyGetter> conflictGetters,
                LogicalDeletedInfo conflictPredicate,
                List<PropertyGetter> updatedGetters,
                boolean updateIgnored,
                Predicate userOptimisticLockPredicate,
                PropertyGetter versionGetter,
                boolean fakeUpdate
        ) {
            if (generatedIdProp != null && generatedIdProp.isEmbedded(EmbeddedLevel.SCALAR)) {
                throw new IllegalArgumentException("Generated id prop cannot be embeddable");
            }
            if (generatedIdProp != null && (
                    userOptimisticLockPredicate != null || versionGetter != null)
            ) {
                throw new IllegalArgumentException(
                        "Optimistic lock is not support by upsert statement which can generate id"
                );
            }
            this.builder = builder;
            this.tableType = tableType;
            this.sequenceIdGenerator = sequenceIdGenerator;
            this.generatedIdGetter = generatedIdProp != null ?
                    Shape.fullOf(builder.sqlClient(), tableType.getJavaClass())
                            .getIdGetters()
                            .get(0) :
                    null;
            this.insertedGetters = insertedGetters;
            this.discriminatorGetter = discriminatorProp != null ?
                    PropertyGetter.propertyGetters(ctx.options.getSqlClient(), discriminatorProp).get(0) :
                    null;
            this.updateDiscriminator = updateDiscriminator;
            this.discriminatorGuardGetter = discriminatorGuardProp != null ?
                    PropertyGetter.propertyGetters(ctx.options.getSqlClient(), discriminatorGuardProp).get(0) :
                    null;
            this.nullGetters = nullGetters;
            this.conflictGetters = conflictGetters;
            this.conflictPredicate = conflictPredicate;
            this.updatedGetters = updatedGetters;
            this.updateIgnored = updateIgnored;
            this.fakeUpdate = fakeUpdate;
            this.userOptimisticLockPredicate = userOptimisticLockPredicate;
            this.versionGetter = versionGetter;
        }

        @Override
        public boolean hasUpdatedColumns() {
            return !updateIgnored &&
                    (!updatedGetters.isEmpty() ||
                            (updateDiscriminator && discriminatorGetter != null) ||
                            !nullGetters.isEmpty());
        }

        @Override
        public boolean hasUpdateCondition() {
            return userOptimisticLockPredicate != null ||
                    versionGetter != null ||
                    discriminatorGuardGetter != null;
        }

        @Override
        public boolean hasGeneratedId() {
            return generatedIdGetter != null;
        }

        @Override
        public boolean isFakeUpdateRequired() {
            return fakeUpdate;
        }

        @Override
        public boolean isUpdateIgnored() {
            return updateIgnored;
        }

        @Override
        public boolean isComplete() {
            Boolean complete = this.complete;
            if (complete == null) {
                this.complete = complete = isComplete0();
            }
            return complete;
        }

        private boolean isComplete0() {
            if (discriminatorGetter != null && !updateDiscriminator) {
                return false;
            }
            for (PropertyGetter getter : insertedGetters) {
                if (!conflictGetters.contains(getter) && !updatedGetters.contains(getter)) {
                    return false;
                }
            }
            if (updatedGetters.isEmpty()) {
                return false;
            }
            for (PropertyGetter getter : updatedGetters) {
                if (!insertedGetters.contains(getter)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isIdInteger() {
            if (generatedIdGetter == null) {
                return false;
            }
            return Classes.INT_TYPES.contains(generatedIdGetter.prop().getReturnClass());
        }

        @Override
        public boolean hasConflictPredicate() {
            return conflictPredicate != null;
        }

        @Override
        public List<ValueGetter> getConflictGetters() {
            return Collections.unmodifiableList(conflictGetters);
        }

        @Override
        public Dialect.UpsertContext sql(String sql) {
            builder.sql(sql);
            return this;
        }

        @Override
        public Dialect.UpsertContext sql(ValueGetter getter) {
            builder.sql(getter);
            return this;
        }

        @Override
        public Dialect.UpsertContext enter(AbstractSqlBuilder.ScopeType type) {
            builder.enter(type);
            return this;
        }

        @Override
        public Dialect.UpsertContext separator() {
            builder.separator();
            return this;
        }

        @Override
        public Dialect.UpsertContext leave() {
            builder.leave();
            return this;
        }

        private String tableName() {
            return tableType.getTableName(ctx.options.getSqlClient().getMetadataStrategy());
        }

        @Override
        public Dialect.UpsertContext appendTableName() {
            builder.sql(tableName());
            return this;
        }

        @Override
        public Dialect.UpsertContext appendInsertedColumns(String prefix) {
            if (sequenceIdGenerator != null) {
                builder.separator()
                        .sql("(")
                        .sql(
                                builder.sqlClient()
                                        .getDialect()
                                        .getSelectIdFromSequenceSql(sequenceIdGenerator.getSequenceName())
                        )
                        .sql(")");
            }
            for (PropertyGetter getter : insertedGetters) {
                if (getter.prop().isId() && sequenceIdGenerator == null) {
                    builder.separator().sql(prefix).sql(getter);
                }
            }
            if (discriminatorGetter != null) {
                builder.separator().sql(prefix).sql(discriminatorGetter);
            }
            for (PropertyGetter getter : insertedGetters) {
                if (!getter.prop().isId()) {
                    builder.separator().sql(prefix).sql(getter);
                }
            }
            for (PropertyGetter getter : nullGetters) {
                builder.separator().sql(prefix).sql(getter);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendConflictColumns() {
            for (PropertyGetter getter : conflictGetters) {
                builder.separator().sql(getter);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendConflictPredicate(String alias) {
            if (conflictPredicate != null) {
                builder.logicalDeleteConflictPredicate(conflictPredicate, alias);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendInsertingValues() {
            builder.enter(BatchSqlBuilder.ScopeType.COMMA);
            for (PropertyGetter getter : insertedGetters) {
                if (getter.prop().isId()) {
                    builder.separator().variable(getter);
                }
            }
            if (discriminatorGetter != null) {
                builder.separator().variable(discriminatorGetter);
            }
            for (PropertyGetter getter : insertedGetters) {
                if (!getter.prop().isId()) {
                    builder.separator().variable(getter);
                }
            }
            for (PropertyGetter ignored : nullGetters) {
                builder.separator().sql("null");
            }
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendUpdatingAssignments(String prefix, String suffix) {
            if (updateDiscriminator && discriminatorGetter != null) {
                builder.separator()
                        .sql(discriminatorGetter)
                        .sql(" = ")
                        .variable(discriminatorGetter);
            }
            for (PropertyGetter getter : nullGetters) {
                builder.separator()
                        .sql(getter)
                        .sql(" = null");
            }
            for (PropertyGetter getter : updatedGetters) {
                builder.separator()
                        .sql(getter)
                        .sql(" = ");
                appendUpdatingValue(getter, prefix, suffix);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendConditionalUpdatingAssignments(
                String sourcePrefix,
                String sourceSuffix,
                String valuePrefix,
                String valueSuffix
        ) {
            if (updateDiscriminator && discriminatorGetter != null) {
                appendConditionalUpdatingAssignment(discriminatorGetter, true, sourcePrefix, sourceSuffix, () -> {
                    builder.variable(discriminatorGetter);
                });
            }
            for (PropertyGetter getter : nullGetters) {
                appendConditionalUpdatingAssignment(getter, true, sourcePrefix, sourceSuffix, () -> {
                    builder.sql("null");
                });
            }
            for (PropertyGetter getter : updatedGetters) {
                appendConditionalUpdatingAssignment(getter, true, sourcePrefix, sourceSuffix, () -> {
                    appendUpdatingValue(getter, valuePrefix, valueSuffix);
                });
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendUpdateCondition(
                String targetPrefix,
                String targetSuffix,
                String sourcePrefix,
                String sourceSuffix
        ) {
            boolean hasPrevious = false;
            if (userOptimisticLockPredicate != null) {
                builder.pushValueGetterRender(targetPrefix, targetSuffix);
                builder.pushOptimisticLockNewValueRender(null, "");
                try {
                    AbstractExpression.renderChild(
                            (Ast) userOptimisticLockPredicate,
                            ExpressionPrecedences.AND,
                            builder
                    );
                } finally {
                    builder.popOptimisticLockNewValueRender();
                    builder.popValueGetterRender();
                }
                hasPrevious = true;
            }
            if (versionGetter != null) {
                if (hasPrevious) {
                    builder.sql(" and ");
                }
                builder
                        .sql(targetPrefix)
                        .sql(versionGetter)
                        .sql(targetSuffix)
                        .sql(" = ")
                        .sql(sourcePrefix)
                        .sql(versionGetter)
                        .sql(sourceSuffix);
                hasPrevious = true;
            }
            if (discriminatorGuardGetter != null) {
                if (hasPrevious) {
                    builder.sql(" and ");
                }
                builder
                        .sql(targetPrefix)
                        .sql(discriminatorGuardGetter)
                        .sql(targetSuffix)
                        .sql(" = ")
                        .sql(sourcePrefix)
                        .sql(discriminatorGuardGetter)
                        .sql(sourceSuffix);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendUpdateConditionWithTableName(
                String sourcePrefix,
                String sourceSuffix
        ) {
            return appendUpdateCondition(tableName() + ".", "", sourcePrefix, sourceSuffix);
        }

        private void appendConditionalUpdatingAssignment(
                PropertyGetter getter,
                boolean useIf,
                String sourcePrefix,
                String sourceSuffix,
                Runnable valueAppender
        ) {
            builder.separator()
                    .sql(getter)
                    .sql(" = ");
            if (useIf) {
                builder.sql("if(");
                appendUpdateCondition("", "", sourcePrefix, sourceSuffix);
                builder.sql(", ");
                valueAppender.run();
                builder.sql(", ").sql(getter).sql(")");
            } else {
                valueAppender.run();
            }
        }

        private void appendUpdatingValue(PropertyGetter getter, String prefix, String suffix) {
            if (getter.metadata().getValueProp().isVersion() && ctx.options.getUserOptimisticLock(ctx.path.getType()) == null) {
                builder.sql(prefix)
                        .sql(getter)
                        .sql(suffix)
                        .sql(" + 1");
            } else {
                builder.sql(prefix)
                        .sql(getter)
                        .sql(suffix);
            }
        }

        @Override
        public Dialect.UpsertContext appendGeneratedId() {
            if (generatedIdGetter != null) {
                builder.sql(generatedIdGetter);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendId() {
            builder.sql(
                    Shape.fullOf(builder.sqlClient(), tableType.getJavaClass())
                            .getIdGetters()
                            .get(0)
            );
            return this;
        }
    }

    private static class TypeChangeRows {

        static final TypeChangeRows EMPTY = new TypeChangeRows(Collections.emptyMap());

        final Map<ImmutableType, Set<Object>> oldTypeIdMap;

        TypeChangeRows(Map<ImmutableType, Set<Object>> oldTypeIdMap) {
            this.oldTypeIdMap = oldTypeIdMap;
        }

        Set<Object> ids() {
            Set<Object> ids = new LinkedHashSet<>();
            for (Set<Object> typeIds : oldTypeIdMap.values()) {
                ids.addAll(typeIds);
            }
            return ids;
        }

        TypeChangeRows filteredBy(Set<Object> ids) {
            if (ids.isEmpty() || oldTypeIdMap.isEmpty()) {
                return EMPTY;
            }
            Map<ImmutableType, Set<Object>> filteredMap = new LinkedHashMap<>();
            for (Map.Entry<ImmutableType, Set<Object>> e : oldTypeIdMap.entrySet()) {
                Set<Object> filteredIds = new LinkedHashSet<>();
                for (Object id : e.getValue()) {
                    if (ids.contains(id)) {
                        filteredIds.add(id);
                    }
                }
                if (!filteredIds.isEmpty()) {
                    filteredMap.put(e.getKey(), filteredIds);
                }
            }
            return filteredMap.isEmpty() ? EMPTY : new TypeChangeRows(filteredMap);
        }
    }

    static class MutationRows {

        static final MutationRows UNKNOWN = new MutationRows(null);

        static final MutationRows EMPTY = new MutationRows(new EntityList<>());

        @Nullable
        final EntityCollection<DraftSpi> acceptedDrafts;

        private MutationRows(@Nullable EntityCollection<DraftSpi> acceptedDrafts) {
            this.acceptedDrafts = acceptedDrafts;
        }

        static MutationRows accepted(EntityCollection<DraftSpi> acceptedDrafts) {
            return acceptedDrafts.isEmpty() ? EMPTY : new MutationRows(acceptedDrafts);
        }
    }
}
