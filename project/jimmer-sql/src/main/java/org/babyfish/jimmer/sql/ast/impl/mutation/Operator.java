package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
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

class Operator {

    private static final String GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON =
            "Joining is disabled in general optimistic lock";

    private static final int[] SIMPLE_ILLEGAL_ROW_COUNTS = new int[]{-1};

    private static final int[] EMPTY_ROW_COUNTS = new int[0];

    final SaveContext ctx;

    Operator(SaveContext ctx) {
        this.ctx = ctx;
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

    public void insert(Batch<DraftSpi> batch) {
        if (batch.entities().isEmpty()) {
            return;
        }
        ImmutableType type = batch.shape().getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                inheritanceInfo.getRootType() != type) {
            insertJoined(batch, inheritanceInfo);
            return;
        }
        insert(
                batch,
                ctx.path.getType(),
                discriminatorProp(inheritanceInfo),
                false
        );
    }

    private void insertJoined(Batch<DraftSpi> batch, InheritanceInfo inheritanceInfo) {
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

    public void update(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch
    ) {
        if (batch.entities().isEmpty()) {
            return;
        }
        ImmutableType type = batch.shape().getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                inheritanceInfo.getRootType() != type) {
            updateJoined(originalIdObjMap, originalKeyObjMap, batch, inheritanceInfo);
            return;
        }
        update(
                originalIdObjMap,
                originalKeyObjMap,
                batch,
                ctx.path.getType(),
                discriminatorProp(inheritanceInfo),
                redundantSingleTableGetters(inheritanceInfo, type)
        );
    }

    private void updateJoined(
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
                return;
            }
        }
        update(
                originalIdObjMap,
                originalKeyObjMap,
                rootBatch,
                rootType,
                discriminatorProp(inheritanceInfo),
                Collections.emptyList()
        );
        deleteRedundantJoinedRows(batch, inheritanceInfo);
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
            saveJoinedTableForUpdate(originalIdObjMap, originalKeyObjMap, batchOf(batch, shape), tableType);
            previousTableType = tableType;
        }
    }

    private void saveJoinedTableForUpdate(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch,
            ImmutableType tableType
    ) {
        if (ctx.options.getSqlClient().getDialect().isUpsertSupported()) {
            upsert(batch, tableType, null, Collections.emptyList(), false);
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
                    Collections.emptyList()
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

    private void update(
            Map<Object, ImmutableSpi> originalIdObjMap,
            Map<KeyMatcher.Group, Map<Object, ImmutableSpi>> originalKeyObjMap,
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            List<PropertyGetter> nullGetters
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
            return;
        }

        if (ctx.options.isIdOnlyAsReference(ctx.path.getProp()) &&
                ctx.options.getUnloadedVersionBehavior(shape.getType()) == UnloadedVersionBehavior.IGNORE &&
                shape.isIdOnly()) {
            return;
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
            return;
        }
        if (keyProps != null && !sqlClient.getDialect().isIdFetchableByKeyUpdate()) {
            fillIds(QueryReason.GET_ID_FOR_KEY_BASE_UPDATE, originalKeyObjMap, batch);
            if (batch.entities().isEmpty()) {
                return;
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
        if (entities == null) {
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

    public void upsert(Batch<DraftSpi> batch, boolean ignoreUpdate) {
        if (batch.entities().isEmpty()) {
            return;
        }
        ImmutableType type = batch.shape().getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                inheritanceInfo.getRootType() != type) {
            upsertJoined(batch, inheritanceInfo, ignoreUpdate);
            return;
        }
        upsert(
                batch,
                ctx.path.getType(),
                discriminatorProp(inheritanceInfo),
                redundantSingleTableGetters(inheritanceInfo, type),
                ignoreUpdate
        );
    }

    private void upsertJoined(Batch<DraftSpi> batch, InheritanceInfo inheritanceInfo, boolean ignoreUpdate) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        DraftSpi sample = batch.entities().iterator().next();
        ImmutableType rootType = inheritanceInfo.getRootType();
        Shape rootShape = Shape.of(
                sqlClient,
                rootType,
                sample,
                prop -> prop.isId() || prop.toOriginal().getDeclaringType().isAssignableFrom(rootType)
        );
        upsert(
                batchOf(batch, rootShape),
                rootType,
                discriminatorProp(inheritanceInfo),
                Collections.emptyList(),
                ignoreUpdate
        );
        deleteRedundantJoinedRows(batch, inheritanceInfo);
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
            upsert(batchOf(batch, shape), tableType, null, Collections.emptyList(), ignoreUpdate);
            previousTableType = tableType;
        }
    }

    private void upsert(
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp discriminatorProp,
            List<PropertyGetter> nullGetters,
            boolean ignoreUpdate
    ) {

        validate(batch.shape(), false);
        if (batch.entities().isEmpty()) {
            return;
        }
        if (ctx.options.isIdOnlyAsReference(ctx.path.getProp()) && batch.shape().isIdOnly()) {
            return;
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
                nullGetters,
                conflictGetters,
                conflictPredicate,
                updatedGetters,
                ignoreUpdate,
                userOptimisticLockPredicate,
                versionGetter
        );
        sqlClient.getDialect().upsert(upsertContext);
        int rowCount = execute(builder, batch, true, ignoreUpdate);
        AffectedRows.add(ctx.affectedRowCountMap, tableType, rowCount);
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

    private void deleteRedundantJoinedRows(Batch<DraftSpi> batch, InheritanceInfo inheritanceInfo) {
        ImmutableType rootType = inheritanceInfo.getRootType();
        ImmutableType targetType = batch.shape().getType();
        Set<ImmutableType> retainedTypes = new HashSet<>(joinedTableTypes(rootType, targetType));
        PropId idPropId = rootType.getIdProp().getId();
        MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
        List<ImmutableType> tableTypes = new ArrayList<>(inheritanceInfo.getConcreteTypes());
        tableTypes.sort((a, b) -> compareJoinedCleanupTableTypes(strategy, a, b));
        for (ImmutableType concreteType : tableTypes) {
            if (concreteType == rootType || !concreteType.isEntity() || retainedTypes.contains(concreteType)) {
                continue;
            }
            BatchSqlBuilder builder = new BatchSqlBuilder(
                    ctx.options.getSqlClient(),
                    batch.entities().size() < 2 || ctx.options.isBatchForbidden()
            );
            builder.sql("delete from ")
                    .sql(concreteType.getTableName(strategy))
                    .sql(" where ")
                    .sql(rootType.getIdProp().<SingleColumn>getStorage(strategy).getName())
                    .sql(" = ")
                    .variable(row -> ((DraftSpi) row).__get(idPropId));
            execute(builder, batch, true, false);
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
                    (!updatedGetters.isEmpty() || discriminatorGetter != null || !nullGetters.isEmpty());
        }

        @Override
        public boolean hasOptimisticLock() {
            return userOptimisticLockPredicate != null || versionGetter != null;
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
                builder.separator()
                        .sql(getter)
                        .sql(" = ");
                if (getter.metadata().getValueProp().isVersion() && ctx.options.getUserOptimisticLock(ctx.path.getType()) == null) {
                    builder.sql(prefix)
                            .sql(getter)
                            .sql(" + 1");
                } else {
                    builder.sql(prefix)
                            .sql(getter)
                            .sql(suffix);
                }
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendOptimisticLockCondition(String sourceTablePrefix) {
            if (userOptimisticLockPredicate != null) {
                ((Ast) userOptimisticLockPredicate).renderTo(builder);
            }
            if (versionGetter != null) {
                builder
                        .sql(ctx.path.getType().getTableName(ctx.options.getSqlClient().getMetadataStrategy()))
                        .sql(".")
                        .sql(versionGetter)
                        .sql(" = ")
                        .sql(sourceTablePrefix)
                        .sql(versionGetter);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendGeneratedId() {
            if (generatedIdGetter != null) {
                builder.sql(generatedIdGetter);
            }
            return this;
        }
    }
}
