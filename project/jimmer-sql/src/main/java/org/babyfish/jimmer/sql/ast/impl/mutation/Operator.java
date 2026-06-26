package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.ForUpdate;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.LockMode;
import org.babyfish.jimmer.sql.ast.query.LockWait;
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
        Shape rootShape = Shape.of(
                sqlClient,
                rootType,
                sample,
                prop -> prop.isId() || prop.toOriginal().getDeclaringType().isAssignableFrom(rootType)
        );
        insert(
                batchOf(batch, rootShape),
                rootType,
                discriminatorProp(inheritanceInfo),
                true
        );
        ImmutableType previousTableType = rootType;
        for (ImmutableType tableType : joinedTableTypes(rootType, batch.shape().getType())) {
            ImmutableType parentTableType = previousTableType;
            Shape shape = Shape.of(
                    sqlClient,
                    tableType,
                    sample,
                    prop -> prop.isId() ||
                            (prop.toOriginal().getDeclaringType().isAssignableFrom(tableType) &&
                                    !prop.toOriginal().getDeclaringType().isAssignableFrom(parentTableType))
            );
            insert(batchOf(batch, shape), tableType, null, true);
            previousTableType = tableType;
        }
        return MutationRows.accepted(batch.entities());
    }

    private void insert(
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            boolean allowIdOnly
    ) {

        if (batch.entities().isEmpty() || (!allowIdOnly && batch.shape().isIdOnly())) {
            return;
        }
        validate(batch.shape(), true);

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
                        ctx.options.getKeyMatcher(tableType)
                );
                conflictProps = new ArrayList<>(keyProps);
                LogicalDeletedInfo logicalDeletedInfo = batch.shape().getType().getLogicalDeletedInfo();
                if (logicalDeletedInfo != null) {
                    conflictProps.add(logicalDeletedInfo.getProp());
                }
            }
        } else {
            upsertMask = null;
            conflictProps = Collections.emptyList();
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
        for (PropertyGetter getter : batch.shape().getGetters()) {
            if (getter.prop().isId() && getter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().sql(getter);
            }
        }
        if (discriminatorGetter != null) {
            builder.separator().sql(discriminatorGetter);
        }
        for (PropertyGetter getter : batch.shape().getGetters()) {
            if (!getter.prop().isId() && getter.isInsertable(conflictProps, upsertMask)) {
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
        for (PropertyGetter getter : batch.shape().getGetters()) {
            if (getter.prop().isId() && getter.isInsertable(conflictProps, upsertMask)) {
                builder.separator().variable(getter);
            }
        }
        if (discriminatorGetter != null) {
            builder.separator().variable(discriminatorGetter);
        }
        for (PropertyGetter getter : batch.shape().getGetters()) {
            if (!getter.prop().isId() && getter.isInsertable(conflictProps, upsertMask)) {
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

        MutationTrigger trigger = ctx.trigger;
        if (trigger != null) {
            for (DraftSpi draft : batch.entities()) {
                trigger.modifyEntityTable(null, draft);
            }
        }
        int rowCount = execute(builder, batch, false, false);
        AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount);
    }

    private ImmutableProp discriminatorProp(@Nullable InheritanceInfo inheritanceInfo) {
        if (inheritanceInfo == null) {
            return null;
        }
        return inheritanceInfo.getDiscriminatorProp();
    }

    private static List<ImmutableType> joinedTableTypes(ImmutableType rootType, ImmutableType type) {
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
        if (inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                inheritanceInfo.getRootType() != type) {
            return updateJoined(originalIdObjMap, originalKeyObjMap, batch, inheritanceInfo);
        }
        update(
                originalIdObjMap,
                originalKeyObjMap,
                batch,
                ctx.path.getType(),
                ctx.options.isSubtypeChangeAllowed() ? discriminatorProp(inheritanceInfo) : null,
                ctx.options.isSubtypeChangeAllowed() ? null : discriminatorProp(inheritanceInfo),
                ctx.options.isSubtypeChangeAllowed() ? redundantSingleTableGetters(inheritanceInfo, type) : Collections.emptyList(),
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
        Shape rootShape = Shape.of(
                sqlClient,
                rootType,
                sample,
                prop -> prop.isId() || prop.toOriginal().getDeclaringType().isAssignableFrom(rootType)
        );
        Batch<DraftSpi> rootBatch = batchOf(batch, rootShape);
        if (rootShape.getIdGetters().isEmpty()) {
            fillIds(QueryReason.GET_ID_FOR_KEY_BASE_UPDATE, originalKeyObjMap, rootBatch);
            if (rootBatch.entities().isEmpty()) {
                return MutationRows.EMPTY;
            }
            sample = rootBatch.entities().iterator().next();
            rootShape = Shape.of(
                    sqlClient,
                    rootType,
                    sample,
                    prop -> prop.isId() || prop.toOriginal().getDeclaringType().isAssignableFrom(rootType)
            );
            rootBatch = batchOf(batch, rootShape);
        }
        int[] acceptedRowCounts = null;
        Batch<DraftSpi> acceptanceBatch = null;
        if (!ctx.options.isSubtypeChangeAllowed() && ownerAcceptanceRequired) {
            acceptanceBatch = rootBatch;
            acceptedRowCounts = fakeRootUpdate(rootBatch, inheritanceInfo);
            batch = batchOfChangedRows(batch, acceptedRowCounts);
            if (batch.entities().isEmpty()) {
                return MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, acceptedRowCounts));
            }
            rootBatch = batchOf(rootBatch, rootShape, batch.entities());
        }
        SubtypeChangeRows subtypeChangeRows = ctx.options.isSubtypeChangeAllowed() ?
                prelockForSubtypeChange(rootBatch, inheritanceInfo) :
                null;
        int[] rootRowCounts = update(
                originalIdObjMap,
                originalKeyObjMap,
                rootBatch,
                rootType,
                ctx.options.isSubtypeChangeAllowed() ? discriminatorProp(inheritanceInfo) : null,
                ctx.options.isSubtypeChangeAllowed() ? null : discriminatorProp(inheritanceInfo),
                Collections.emptyList(),
                !ctx.options.isSubtypeChangeAllowed()
        );
        if (ctx.options.isSubtypeChangeAllowed()) {
            deleteRedundantJoinedRows(batch, inheritanceInfo, subtypeChangeRows);
        }
        ImmutableType previousTableType = rootType;
        for (ImmutableType tableType : joinedTableTypes(rootType, batch.shape().getType())) {
            ImmutableType parentTableType = previousTableType;
            Shape shape = Shape.of(
                    sqlClient,
                    tableType,
                    sample,
                    prop -> prop.isId() ||
                            (prop.toOriginal().getDeclaringType().isAssignableFrom(tableType) &&
                                    !prop.toOriginal().getDeclaringType().isAssignableFrom(parentTableType))
            );
            Batch<DraftSpi> childBatch = batchOf(batch, shape);
            if (ctx.options.isSubtypeChangeAllowed()) {
                saveJoinedTableForUpdate(originalIdObjMap, originalKeyObjMap, childBatch, tableType);
            } else {
                upsertJoinedChildWithRootGuard(childBatch, tableType, rootType, inheritanceInfo, false);
            }
            previousTableType = tableType;
        }
        return acceptedRowCounts != null ?
                MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, acceptedRowCounts)) :
                MutationRows.UNKNOWN;
    }

    private void saveJoinedTableForUpdate(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch,
            ImmutableType tableType
    ) {
        if (ctx.options.getSqlClient().getDialect().isUpsertSupported()) {
            upsert(batch, tableType, null, false, null, Collections.emptyList(), false);
            return;
        }
        Set<Object> existingIds = findExistingIds(tableType, batch);
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
                    originalIdObjMap,
                    originalKeyObjMap,
                    batchOf(batch, batch.shape(), existingEntities),
                    tableType,
                    null,
                    null,
                    Collections.emptyList(),
                    false
            );
        }
        if (!missingEntities.isEmpty()) {
            insert(batchOf(batch, batch.shape(), missingEntities), tableType, null, true);
        }
    }

    private Set<Object> findExistingIds(ImmutableType tableType, Batch<DraftSpi> batch) {
        Set<Object> ids = new LinkedHashSet<>((batch.entities().size() * 4 + 2) / 3);
        PropId idPropId = tableType.getIdProp().getId();
        for (DraftSpi draft : batch.entities()) {
            ids.add(draft.__get(idPropId));
        }
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("select ")
                .definition(tableType.getIdProp().getStorage(sqlClient.getMetadataStrategy()))
                .sql(" from ")
                .sql(tableType.getTableName(sqlClient.getMetadataStrategy()))
                .sql(" where ");
        ComparisonPredicates.renderIn(
                false,
                ValueGetter.valueGetters(sqlClient, tableType.getIdProp()),
                ids,
                builder
        );
        Tuple3<String, List<Object>, List<Integer>> tuple = builder.build();
        Reader<?> reader = sqlClient.getReader(tableType.getIdProp());
        return sqlClient.getExecutor().execute(
                new Executor.Args<>(
                        sqlClient,
                        ctx.con,
                        tuple.get_1(),
                        tuple.get_2(),
                        tuple.get_3(),
                        ExecutionPurpose.command(QueryReason.GET_ID_FOR_KEY_BASE_UPDATE),
                        ctx.options.getExceptionTranslator(),
                        null,
                        (stmt, args) -> {
                            Set<Object> existingIds = new LinkedHashSet<>();
                            Reader.Context readerContext = new Reader.Context(null, sqlClient);
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    readerContext.resetCol();
                                    existingIds.add(reader.read(rs, readerContext));
                                }
                            }
                            return existingIds;
                        }
                )
        );
    }

    private SubtypeChangeRows prelockForSubtypeChange(Batch<DraftSpi> rootBatch, InheritanceInfo inheritanceInfo) {
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
            return SubtypeChangeRows.EMPTY;
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
        sqlClient.getDialect().renderForUpdate(builder, new ForUpdate(LockMode.UPDATE, LockWait.DEFAULT));
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
                        ExecutionPurpose.command(QueryReason.PRELOCK_FOR_SUBTYPE_CHANGE),
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
                                                "Cannot change subtype for joined inheritance rows, " +
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
                                    SubtypeChangeRows.EMPTY :
                                    new SubtypeChangeRows(oldTypeIdMap);
                        }
                )
        );
    }

    private int[] fakeRootUpdate(Batch<DraftSpi> rootBatch, InheritanceInfo inheritanceInfo) {
        if (rootBatch.entities().isEmpty()) {
            return EMPTY_ROW_COUNTS;
        }
        List<PropertyGetter> idGetters = rootBatch.shape().getIdGetters();
        if (idGetters.isEmpty()) {
            throw new ExecutionException(
                    "Cannot gate downstream mutation work for joined inheritance without root id"
            );
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        ImmutableType rootType = inheritanceInfo.getRootType();
        PropertyGetter idGetter = idGetters.get(0);
        PropertyGetter discriminatorGetter = discriminatorGetterForBatch(rootBatch, inheritanceInfo);
        Predicate userOptimisticLockPredicate = userLockOptimisticPredicate();
        PropertyGetter versionGetter = rootBatch.shape().getVersionGetter();

        BatchSqlBuilder builder = new BatchSqlBuilder(
                sqlClient,
                rootBatch.entities().size() < 2 ||
                        ctx.options.isBatchForbidden() ||
                        sqlClient.getDialect().isBatchDumb()
        );
        builder.sql("update ")
                .sql(rootType.getTableName(strategy))
                .enter(BatchSqlBuilder.ScopeType.SET)
                .separator()
                .sql(idGetter)
                .sql(" = ")
                .sql(idGetter)
                .leave()
                .enter(BatchSqlBuilder.ScopeType.WHERE);
        for (PropertyGetter getter : idGetters) {
            builder.separator()
                    .sql(getter)
                    .sql(" = ")
                    .variable(getter);
        }
        builder.separator()
                .sql(discriminatorGetter)
                .sql(" = ")
                .variable(discriminatorGetter);
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
        builder.leave();
        int[] rowCounts = executeGateAndGetRowCounts(builder, rootBatch);
        if (versionGetter != null || userOptimisticLockPredicate != null) {
            int index = 0;
            for (DraftSpi row : rootBatch.entities()) {
                if (rowCounts[index++] == 0) {
                    ctx.throwOptimisticLockError(row);
                }
            }
        }
        return rowCounts;
    }

    private int[] executeGateAndGetRowCounts(BatchSqlBuilder builder, Batch<DraftSpi> batch) {
        if (batch.entities().isEmpty()) {
            return EMPTY_ROW_COUNTS;
        }
        JSqlClientImplementor sqlClient = builder.sqlClient();
        Tuple3<String, BatchSqlBuilder.VariableMapper, List<Integer>> tuple = builder.build();
        String sql = tuple.get_1();
        BatchSqlBuilder.VariableMapper mapper = tuple.get_2();
        if (batch.entities().size() < 2 ||
                ctx.options.isBatchForbidden() ||
                sqlClient.getDialect().isBatchDumb()) {
            int[] rowCounts = new int[batch.entities().size()];
            int index = 0;
            for (DraftSpi draft : batch.entities()) {
                rowCounts[index++] = sqlClient.getExecutor().execute(
                        new Executor.Args<>(
                                sqlClient,
                                ctx.con,
                                sql,
                                mapper.variables(draft),
                                tuple.get_3(),
                                ExecutionPurpose.MUTATE,
                                ctx.options.getExceptionTranslator(),
                                null,
                                (stmt, args) -> stmt.executeUpdate()
                        )
                );
            }
            return rowCounts;
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
            for (DraftSpi draft : batch.entities()) {
                batchContext.add(mapper.variables(draft));
            }
            return batchContext.execute((ex, args) -> convertFinalException(ex, args));
        }
    }

    private int[] update(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            @Nullable ImmutableProp discriminatorGuardProp,
            List<PropertyGetter> nullGetters,
            boolean forceAllRows
    ) {
        Shape shape = batch.shape();
        validate(shape, false);
        KeyMatcher.Group group = shape.getIdGetters().isEmpty() ?
                shape.group(ctx.options.getKeyMatcher(shape.getType())) :
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

        if (ctx.options.isIdOnlyAsReference(ctx.path.getProp()) &&
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
                conflictProps = new ArrayList<>(keyProps);
                LogicalDeletedInfo logicalDeletedInfo = batch.shape().getType().getLogicalDeletedInfo();
                if (logicalDeletedInfo != null) {
                    conflictProps.add(logicalDeletedInfo.getProp());
                }
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
        if (updatedGetters.isEmpty() && discriminatorProp == null && nullGetters.isEmpty() && !hasOptimisticLock) {
            fillIds(QueryReason.GET_ID_WHEN_UPDATE_NOTHING, originalKeyObjMap, batch);
            return EMPTY_ROW_COUNTS;
        }
        if (keyProps != null && !sqlClient.getDialect().isIdFetchableByKeyUpdate()) {
            fillIds(QueryReason.GET_ID_FOR_KEY_BASE_UPDATE, originalKeyObjMap, batch);
            if (batch.entities().isEmpty()) {
                return EMPTY_ROW_COUNTS;
            }
        }
        BatchSqlBuilder builder = new BatchSqlBuilder(
                sqlClient,
                batch.entities().size() < 2 || ctx.options.isBatchForbidden()
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
                nullGetters,
                userOptimisticLockPredicate,
                versionGetter
        );
        sqlClient.getDialect().update(updateContext);

        MutationTrigger trigger = ctx.trigger;
        EntityCollection<DraftSpi> entities = changedProps != null ?
                new EntityList<>(batch.entities().size()) :
                null;
        if (entities != null || trigger != null) {
            if (keyProps != null) {
                Map<Object, ImmutableSpi> subMap = originalIdObjMap != null ?
                        originalKeyObjMap.getOrDefault(group, Collections.emptyMap()) :
                        Collections.emptyMap();
                for (DraftSpi draft : batch.entities()) {
                    ImmutableSpi oldRow = subMap.get(Keys.keyOf(draft, keyProps));
                    if (isChanged(changedProps, oldRow, draft)) {
                        if (trigger != null) {
                            trigger.modifyEntityTable(oldRow, draft);
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
                        if (trigger != null) {
                            trigger.modifyEntityTable(oldRow, draft);
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
        int[] rowCounts = executeAndGetRowCounts(
                builder,
                shape,
                entities,
                true,
                false
        );
        if (versionGetter != null || userOptimisticLockPredicate != null) {
            int index = 0;
            for (DraftSpi row : entities) {
                if (rowCounts[index++] == 0) {
                    ctx.throwOptimisticLockError(row);
                }
            }
        }
        AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount(rowCounts));
        return rowCounts;
    }

    @SuppressWarnings("unchecked")
    private void fillIds(
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
        for (Iterator<DraftSpi> itr = batch.entities().iterator(); itr.hasNext(); ) {
            DraftSpi draft = itr.next();
            ImmutableSpi row = subMap.get(Keys.keyOf(draft, keyProps));
            if (row != null) {
                draft.__set(idPropId, row.__get(idPropId));
            } else {
                itr.remove();
            }
        }
    }

    public MutationRows upsert(Batch<DraftSpi> batch, boolean ignoreUpdate) {
        if (batch.entities().isEmpty()) {
            return MutationRows.EMPTY;
        }
        ImmutableType type = batch.shape().getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                inheritanceInfo.getRootType() != type) {
            return upsertJoined(batch, inheritanceInfo, ignoreUpdate);
        }
        upsert(
                batch,
                ctx.path.getType(),
                discriminatorProp(inheritanceInfo),
                ctx.options.isSubtypeChangeAllowed(),
                ctx.options.isSubtypeChangeAllowed() ? null : discriminatorProp(inheritanceInfo),
                ctx.options.isSubtypeChangeAllowed() ? redundantSingleTableGetters(inheritanceInfo, type) : Collections.emptyList(),
                ignoreUpdate
        );
        return MutationRows.UNKNOWN;
    }

    private MutationRows upsertJoined(Batch<DraftSpi> batch, InheritanceInfo inheritanceInfo, boolean ignoreUpdate) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        DraftSpi sample = batch.entities().iterator().next();
        ImmutableType rootType = inheritanceInfo.getRootType();
        Shape rootShape = Shape.of(
                sqlClient,
                rootType,
                sample,
                prop -> prop.isId() || prop.toOriginal().getDeclaringType().isAssignableFrom(rootType)
        );
        Batch<DraftSpi> rootBatch = batchOf(batch, rootShape);
        SubtypeChangeRows subtypeChangeRows = ctx.options.isSubtypeChangeAllowed() ?
                prelockForSubtypeChange(rootBatch, inheritanceInfo) :
                null;
        int[] rootRowCounts = upsert(
                rootBatch,
                rootType,
                discriminatorProp(inheritanceInfo),
                ctx.options.isSubtypeChangeAllowed(),
                ctx.options.isSubtypeChangeAllowed() ? null : discriminatorProp(inheritanceInfo),
                Collections.emptyList(),
                ignoreUpdate
        );
        int[] acceptedRowCounts = null;
        Batch<DraftSpi> acceptanceBatch = null;
        if (ctx.options.isSubtypeChangeAllowed()) {
            deleteRedundantJoinedRows(batch, inheritanceInfo, subtypeChangeRows);
        } else if (ignoreUpdate) {
            acceptedRowCounts = rootRowCounts;
            acceptanceBatch = rootBatch;
            batch = batchOfChangedRows(batch, rootRowCounts);
            if (batch.entities().isEmpty()) {
                return MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, acceptedRowCounts));
            }
        } else if (ownerAcceptanceRequired) {
            if (rootBatch.shape().getIdGetters().isEmpty()) {
                sample = rootBatch.entities().iterator().next();
                rootShape = Shape.of(
                        sqlClient,
                        rootType,
                        sample,
                        prop -> prop.isId() || prop.toOriginal().getDeclaringType().isAssignableFrom(rootType)
                );
                rootBatch = batchOf(batch, rootShape);
            }
            acceptanceBatch = rootBatch;
            acceptedRowCounts = fakeRootUpdate(rootBatch, inheritanceInfo);
            batch = batchOfChangedRows(batch, acceptedRowCounts);
            if (batch.entities().isEmpty()) {
                return MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, acceptedRowCounts));
            }
        }
        ImmutableType previousTableType = rootType;
        for (ImmutableType tableType : joinedTableTypes(rootType, batch.shape().getType())) {
            ImmutableType parentTableType = previousTableType;
            Shape shape = Shape.of(
                    sqlClient,
                    tableType,
                    sample,
                    prop -> prop.isId() ||
                            (prop.toOriginal().getDeclaringType().isAssignableFrom(tableType) &&
                                    !prop.toOriginal().getDeclaringType().isAssignableFrom(parentTableType))
            );
            Batch<DraftSpi> childBatch = batchOf(batch, shape);
            if (ctx.options.isSubtypeChangeAllowed()) {
                upsert(childBatch, tableType, null, false, null, Collections.emptyList(), ignoreUpdate);
            } else {
                upsertJoinedChildWithRootGuard(childBatch, tableType, rootType, inheritanceInfo, ignoreUpdate);
            }
            previousTableType = tableType;
        }
        return acceptedRowCounts != null ?
                MutationRows.accepted(acceptedOriginalEntities(acceptanceBatch, acceptedRowCounts)) :
                MutationRows.UNKNOWN;
    }

    private void upsertJoinedChildWithRootGuard(
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            ImmutableType rootType,
            InheritanceInfo inheritanceInfo,
            boolean ignoreUpdate
    ) {
        if (batch.entities().isEmpty()) {
            return;
        }
        if (!ignoreUpdate) {
            updateJoinedChildWithRootGuard(batch, tableType, rootType, inheritanceInfo);
        }
        insertJoinedChildIfAbsentWithRootGuard(batch, tableType, rootType, inheritanceInfo);
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
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        List<PropertyGetter> idGetters = Shape.fullOf(sqlClient, tableType.getJavaClass()).getIdGetters();
        PropertyGetter discriminatorGetter = discriminatorGetterForBatch(batch, inheritanceInfo);
        String rootTableName = rootType.getTableName(strategy);
        String childTableName = tableType.getTableName(strategy);
        String rootIdColumnName = rootType.getIdProp().<SingleColumn>getStorage(strategy).getName();
        String discriminatorColumnName = inheritanceInfo
                .getDiscriminatorProp()
                .<SingleColumn>getStorage(strategy)
                .getName();

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
        builder.separator()
                .sql("exists(select 1 from ")
                .sql(rootTableName)
                .sql(" where ")
                .sql(rootTableName)
                .sql(".")
                .sql(rootIdColumnName)
                .sql(" = ");
        for (PropertyGetter idGetter : idGetters) {
            builder.variable(idGetter);
        }
        builder.sql(" and ")
                .sql(discriminatorColumnName)
                .sql(" = ")
                .variable(discriminatorGetter)
                .sql(")");
        builder.leave();
        int rowCount = execute(builder, batch, true, false);
        AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount);
    }

    private void insertJoinedChildIfAbsentWithRootGuard(
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            ImmutableType rootType,
            InheritanceInfo inheritanceInfo
    ) {
        if (batch.entities().isEmpty()) {
            return;
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        Shape fullShape = Shape.fullOf(sqlClient, tableType.getJavaClass());
        List<PropertyGetter> insertedGetters = new ArrayList<>();
        insertedGetters.addAll(fullShape.getIdGetters());
        for (PropertyGetter getter : batch.shape().getColumnDefinitionGetters()) {
            if (!getter.prop().isId()) {
                insertedGetters.add(getter);
            }
        }
        List<PropertyGetter> defaultGetters = new ArrayList<>();
        for (PropertyGetter getter : fullShape.getColumnDefinitionGetters()) {
            if (!getter.prop().isId() && getter.metadata().hasDefaultValue() && !batch.shape().contains(getter)) {
                defaultGetters.add(getter);
            }
        }
        insertedGetters.addAll(defaultGetters);

        List<PropertyGetter> idGetters = fullShape.getIdGetters();
        PropertyGetter discriminatorGetter = discriminatorGetterForBatch(batch, inheritanceInfo);
        String rootTableName = rootType.getTableName(strategy);
        String childTableName = tableType.getTableName(strategy);
        String rootIdColumnName = rootType.getIdProp().<SingleColumn>getStorage(strategy).getName();
        String childIdColumnName = tableType.getIdProp().<SingleColumn>getStorage(strategy).getName();
        String discriminatorColumnName = inheritanceInfo
                .getDiscriminatorProp()
                .<SingleColumn>getStorage(strategy)
                .getName();

        BatchSqlBuilder builder = new BatchSqlBuilder(
                sqlClient,
                batch.entities().size() < 2 || ctx.options.isBatchForbidden()
        );
        builder.sql("insert into ")
                .sql(childTableName)
                .enter(BatchSqlBuilder.ScopeType.TUPLE);
        for (PropertyGetter getter : insertedGetters) {
            builder.separator().sql(getter);
        }
        builder.leave()
                .sql(" select ");
        boolean addComma = false;
        for (PropertyGetter getter : insertedGetters) {
            if (addComma) {
                builder.sql(", ");
            } else {
                addComma = true;
            }
            if (defaultGetters.contains(getter)) {
                builder.defaultVariable(getter);
            } else {
                builder.variable(getter);
            }
        }
        String constantTableName = sqlClient.getDialect().getConstantTableName();
        if (constantTableName != null) {
            builder.sql(" from ").sql(constantTableName);
        }
        builder.sql(" where exists(select 1 from ")
                .sql(rootTableName)
                .sql(" where ")
                .sql(rootTableName)
                .sql(".")
                .sql(rootIdColumnName)
                .sql(" = ");
        for (PropertyGetter idGetter : idGetters) {
            builder.variable(idGetter);
        }
        builder.sql(" and ")
                .sql(discriminatorColumnName)
                .sql(" = ")
                .variable(discriminatorGetter)
                .sql(") and not exists(select 1 from ")
                .sql(childTableName)
                .sql(" where ")
                .sql(childIdColumnName)
                .sql(" = ");
        for (PropertyGetter idGetter : idGetters) {
            builder.variable(idGetter);
        }
        builder.sql(")");
        int rowCount = execute(builder, batch, false, false);
        AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount);
    }

    private PropertyGetter discriminatorGetterForBatch(Batch<DraftSpi> batch, InheritanceInfo inheritanceInfo) {
        ImmutableProp discriminatorProp = inheritanceInfo.getDiscriminatorProp();
        ImmutableProp prop = batch.shape().getType().getProps().get(discriminatorProp.getName());
        return PropertyGetter
                .propertyGetters(ctx.options.getSqlClient(), prop != null ? prop : discriminatorProp)
                .get(0);
    }

    private int[] upsert(
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            boolean updateDiscriminator,
            @Nullable ImmutableProp discriminatorGuardProp,
            List<PropertyGetter> nullGetters,
            boolean ignoreUpdate
    ) {

        validate(batch.shape(), false);
        if (batch.entities().isEmpty()) {
            return EMPTY_ROW_COUNTS;
        }
        if (ctx.options.isIdOnlyAsReference(ctx.path.getProp()) && batch.shape().isIdOnly()) {
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
                    ctx.options.getKeyMatcher(tableType)
            );
            conflictProps = new ArrayList<>(keyProps);
            LogicalDeletedInfo logicalDeletedInfo = batch.shape().getType().getLogicalDeletedInfo();
            boolean filteredLogicalDeletedKey = isFilteredLogicalDeletedKey(batch.shape().getType());
            if (logicalDeletedInfo != null) {
                conflictProps.add(logicalDeletedInfo.getProp());
            }
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

        BatchSqlBuilder builder = new BatchSqlBuilder(
                sqlClient,
                batch.entities().size() < 2 || ctx.options.isBatchForbidden()
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
                versionGetter
        );
        sqlClient.getDialect().upsert(upsertContext);
        int[] rowCounts = executeAndGetRowCounts(
                builder,
                batch.shape(),
                batch.entities(),
                true,
                ignoreUpdate
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
            @Nullable SubtypeChangeRows subtypeChangeRows
    ) {
        if (subtypeChangeRows == null || subtypeChangeRows.oldTypeIdMap.isEmpty()) {
            return;
        }
        ImmutableType rootType = inheritanceInfo.getRootType();
        ImmutableType targetType = batch.shape().getType();
        Set<ImmutableType> retainedTypes = new HashSet<>(joinedTableTypes(rootType, targetType));
        MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
        Map<ImmutableType, Set<Object>> tableIdMap = new LinkedHashMap<>();
        for (Map.Entry<ImmutableType, Set<Object>> e : subtypeChangeRows.oldTypeIdMap.entrySet()) {
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
        Set<ImmutableProp> keyProps = shape.keyProps(ctx.options.getKeyMatcher(shape.getType()));
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
        return userOptimisticLock.predicate(
                (Table<Object>) table,
                OptimisticLockValueFactoryFactories.<Object>of()
        );
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
        if (entities.isEmpty()) {
            return EMPTY_ROW_COUNTS;
        }
        if (entities.size() < 2 || ctx.options.isBatchForbidden() || isForcedOneByOne(shape, entities)) {
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

        private final List<PropertyGetter> nullGetters;

        private final Predicate userOptimisticLockPredicate;

        private final PropertyGetter versionGetter;

        UpdateContextImpl(
                BatchSqlBuilder builder,
                ImmutableType tableType,
                Shape shape,
                PropertyGetter idGetter,
                Set<ImmutableProp> keyProps,
                List<PropertyGetter> updatedGetters,
                @Nullable ImmutableProp discriminatorProp,
                @Nullable ImmutableProp discriminatorGuardProp,
                List<PropertyGetter> nullGetters,
                Predicate userOptimisticLockPredicate,
                PropertyGetter versionGetter
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
                ImmutableProp prop = shape.getType().getProps().get(discriminatorGuardProp.getName());
                this.discriminatorGuardGetter = PropertyGetter
                        .propertyGetters(ctx.options.getSqlClient(), prop != null ? prop : discriminatorGuardProp)
                        .get(0);
            } else {
                this.discriminatorGuardGetter = null;
            }
            this.nullGetters = nullGetters;
            this.userOptimisticLockPredicate = userOptimisticLockPredicate;
            this.versionGetter = versionGetter;
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
            if (discriminatorGetter != null) {
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
                if (getter != versionGetter) {
                    builder.separator()
                            .sql(getter)
                            .sql(" = ")
                            .variable(getter);
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
                        .sql(" = ")
                        .variable(discriminatorGuardGetter);
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
                PropertyGetter versionGetter
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

        @Override
        public Dialect.UpsertContext appendTableName() {
            builder.sql(tableType.getTableName(ctx.options.getSqlClient().getMetadataStrategy()));
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
                ((Ast) userOptimisticLockPredicate).renderTo(builder);
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
    }

    private static class SubtypeChangeRows {

        static final SubtypeChangeRows EMPTY = new SubtypeChangeRows(Collections.emptyMap());

        final Map<ImmutableType, Set<Object>> oldTypeIdMap;

        SubtypeChangeRows(Map<ImmutableType, Set<Object>> oldTypeIdMap) {
            this.oldTypeIdMap = oldTypeIdMap;
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
