package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.DiscriminatorColumn;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.JoinedTableDeleteMode;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.Variables;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.ForUpdate;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.LockMode;
import org.babyfish.jimmer.sql.ast.query.LockWait;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.impl.LogicalDeletedValueGenerators;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;

public class Deleter {

    private final DeleteContext ctx;

    private Set<Object> ids;

    private Map<Object, ImmutableSpi> rowMap;

    public Deleter(
            ImmutableType type,
            DeleteOptions options,
            Connection con,
            MutationTrigger trigger,
            Map<AffectedTable, Integer> affectedRowCountMap
    ) {
        this.ctx = new DeleteContext(
                options,
                con,
                trigger,
                affectedRowCountMap,
                MutationPath.root(type)
        );
    }

    public void addIds(Collection<Object> ids) {
        if (ids.isEmpty()) {
            return;
        }
        if (rowMap != null && !rowMap.isEmpty()) {
            throw new IllegalStateException("addRows has been called");
        }
        Set<Object> set = this.ids;
        if (set == null) {
            this.ids = set = new LinkedHashSet<>((ids.size() * 4 + 2) / 3);
        }
        Class<?> boxedIdType = Classes.boxTypeOf(ctx.path.getType().getIdProp().getReturnClass());
        for (Object id : ids) {
            if (id == null) {
                continue;
            }
            if (!boxedIdType.isAssignableFrom(id.getClass())) {
                throw new IllegalArgumentException(
                        "Illegal id \"" +
                                id +
                                "\", the expected id type is \"" +
                                boxedIdType.getName() +
                                "\" but the actual id type is \"" +
                                id.getClass().getName() +
                                "\""
                );
            }
            set.add(id);
        }
    }

    public void addRows(Collection<ImmutableSpi> rows) {
        if (rows.isEmpty()) {
            return;
        }
        if (ids != null && !ids.isEmpty()) {
            throw new IllegalStateException("addIds has been called");
        }
        ImmutableType type = ctx.path.getType();
        PropId idPropId = type.getIdProp().getId();
        Map<Object, ImmutableSpi> rowMap = this.rowMap;
        if (rowMap == null) {
            this.rowMap = rowMap = new LinkedHashMap<>((rows.size() * 4 + 2) / 3);
        }
        for (ImmutableSpi row : rows) {
            if (!type.isAssignableFrom(row.__type())) {
                throw new IllegalArgumentException(
                        "Illegal row \"" +
                                row +
                                "\", the expected id type is \"" +
                                type +
                                "\" but the actual id type is \"" +
                                row.__type() +
                                "\""
                );
            }
            rowMap.put(row.__get(idPropId), row);
        }
    }

    public DeleteResult execute() {

        Set<Object> ids = this.ids;
        Map<Object, ImmutableSpi> rowMap = this.rowMap;
        if (ids == null && rowMap == null) {
            return new DeleteResult(Collections.emptyMap());
        }
        this.ids = null;
        this.rowMap = null;

        if (ids == null) {
            ids = rowMap.keySet();
        }

        SaveContext saveCtx = new SaveContext(
                saveOptions(),
                ctx.con,
                ctx.path.getType(),
                null,
                ctx.trigger,
                ctx.affectedRowCountMap
        );
        List<MiddleTableOperator> middleOperators = AbstractAssociationOperator.createMiddleTableOperators(
                ctx.options.getSqlClient(),
                ctx.path,
                ctx.isLogicalDeleted() ? DisconnectingType.LOGICAL_DELETE : DisconnectingType.PHYSICAL_DELETE,
                prop -> new MiddleTableOperator(saveCtx.prop(prop), ctx.isLogicalDeleted()),
                backProp -> new MiddleTableOperator(saveCtx.backProp(backProp), ctx.isLogicalDeleted())
        );
        for (MiddleTableOperator middleTableOperator : middleOperators) {
            middleTableOperator.disconnect(ids);
        }
        List<ChildTableOperator> subOperators = AbstractAssociationOperator.createSubOperators(
                ctx.options.getSqlClient(),
                ctx.path,
                ctx.isLogicalDeleted() ? DisconnectingType.LOGICAL_DELETE : DisconnectingType.PHYSICAL_DELETE,
                backProp -> new ChildTableOperator(ctx.backPropOf(backProp), false)
        );
        for (ChildTableOperator subOperator : subOperators) {
            subOperator.disconnect(ids);
        }

        int rowCount = executeImpl(ids, rowMap, ctx.getOptions().getExceptionTranslator());
        if (ctx.trigger != null) {
            ctx.trigger.submit(ctx.options.getSqlClient(), ctx.con);
        }

        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
        return new DeleteResult(ctx.affectedRowCountMap);
    }

    private int executeImpl(
            Collection<Object> ids,
            Map<Object, ImmutableSpi> rowMap,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        return delete(
                ctx.options.getSqlClient(),
                ctx.con,
                ctx.path.getType(),
                ids,
                rowMap,
                ctx.trigger,
                ctx.isLogicalDeleted(),
                exceptionTranslator
        );
    }

    private static int delete(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Collection<Object> ids,
            Map<Object, ImmutableSpi> rowMap,
            MutationTrigger trigger,
            boolean logicalDeleted,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        LogicalDeletedInfo info = logicalDeleted ? type.getLogicalDeletedInfo() : null;
        LogicalDeletedValueGenerator<?> generator =
                LogicalDeletedValueGenerators.of(info, sqlClient);
        if (trigger != null) {
            return deleteWithTrigger(
                    sqlClient,
                    con,
                    type,
                    ids,
                    rowMap,
                    trigger,
                    info,
                    generator != null ? generator.generate() : null,
                    exceptionTranslator
            );
        }
        return deleteWithoutTrigger(
                sqlClient,
                con,
                type,
                ids != null ? ids : rowMap.keySet(),
                info,
                generator != null ? generator.generate() : null,
                exceptionTranslator
        );
    }

    private static int deleteWithTrigger(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Collection<Object> ids,
            Map<Object, ImmutableSpi> rowMap,
            MutationTrigger trigger,
            LogicalDeletedInfo info,
            Object generatedValue,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        if (rowMap == null) {
            MutableRootQueryImpl<Table<?>> q = new MutableRootQueryImpl<>(
                    sqlClient,
                    type,
                    ExecutionPurpose.command(QueryReason.TRIGGER),
                    info != null ? FilterLevel.IGNORE_USER_FILTERS : FilterLevel.IGNORE_ALL
            );
            Table<ImmutableSpi> t = q.getTable();
            q.where(t.get(type.getIdProp().getName()).in(ids));
            List<ImmutableSpi> rows = q.select(t).execute(con);
            rowMap = new LinkedHashMap<>((rows.size() * 4 + 2) / 3);
            PropId idPropId = type.getIdProp().getId();
            for (ImmutableSpi row : rows) {
                rowMap.put(row.__get(idPropId), row);
            }
        }
        if (rowMap.isEmpty()) {
            return 0;
        }
        for (ImmutableSpi row : rowMap.values()) {
            if (info != null) {
                fireEvent(row, info.getProp(), generatedValue, trigger);
            } else {
                fireEvent(row, null, null, trigger);
            }
        }
        return deleteWithoutTrigger(sqlClient, con, type, rowMap.keySet(), info, generatedValue, exceptionTranslator);
    }

    private static int deleteWithoutTrigger(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Collection<Object> ids,
            LogicalDeletedInfo info,
            Object generatedDeletedValue,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo != null) {
            ImmutableType rootType = inheritanceInfo.getRootType();
            if (info != null) {
                return deleteFromSingleTable(
                        sqlClient,
                        con,
                        rootType,
                        type,
                        ids,
                        info,
                        generatedDeletedValue,
                        exceptionTranslator
                );
            }
            if (inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                    inheritanceInfo.getJoinedTableDeleteMode() != JoinedTableDeleteMode.DB_CASCADE) {
                LockedJoinedRows lockedRows = lockJoinedRows(
                        sqlClient,
                        con,
                        inheritanceInfo,
                        type,
                        ids,
                        exceptionTranslator
                );
                if (lockedRows.ids.isEmpty()) {
                    return 0;
                }
                deleteJoinedSubtypeTables(
                        sqlClient,
                        con,
                        lockedRows.tableIdMap,
                        exceptionTranslator
                );
                ids = lockedRows.ids;
            }
            return deleteFromSingleTable(
                    sqlClient,
                    con,
                    rootType,
                    type,
                    ids,
                    null,
                    null,
                    exceptionTranslator
            );
        }
        return deleteFromSingleTable(
                sqlClient,
                con,
                type,
                type,
                ids,
                info,
                generatedDeletedValue,
                exceptionTranslator
        );
    }

    private static int deleteFromSingleTable(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType tableType,
            ImmutableType deletedType,
            Collection<Object> ids,
            LogicalDeletedInfo info,
            Object generatedDeletedValue,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        if (info != null) {
            builder.sql("update ")
                    .sql(tableType.getTableName(sqlClient.getMetadataStrategy()))
                    .sql(" set ")
                    .sql(info.getProp().<SingleColumn>getStorage(sqlClient.getMetadataStrategy()).getName())
                    .sql(" = ");
            if (generatedDeletedValue != null) {
                builder.rawVariable(Variables.process(generatedDeletedValue, info.getProp(), sqlClient));
            } else {
                builder.sql("null");
            }
        } else {
            builder.sql("delete from ")
                    .sql(tableType.getTableName(sqlClient.getMetadataStrategy()));
        }
        builder.sql(" where ");
        ComparisonPredicates.renderIn(
                false,
                ValueGetter.valueGetters(sqlClient, tableType.getIdProp()),
                ids,
                builder
        );
        renderDiscriminatorPredicate(builder, tableType, deletedType);
        return execute(sqlClient, con, builder, exceptionTranslator);
    }

    private static LockedJoinedRows lockJoinedRows(
            JSqlClientImplementor sqlClient,
            Connection con,
            InheritanceInfo inheritanceInfo,
            ImmutableType deletedType,
            Collection<Object> ids,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        ImmutableType rootType = inheritanceInfo.getRootType();
        DiscriminatorColumn discriminatorColumn = inheritanceInfo.getDiscriminatorColumn();
        if (discriminatorColumn == null) {
            throw new ExecutionException(
                    "Cannot physically delete joined inheritance rows explicitly because \"" +
                            rootType +
                            "\" does not have discriminator column metadata"
            );
        }
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        ImmutableProp idProp = rootType.getIdProp();
        boolean readDiscriminator = deletedType == rootType || hasEntityDerivedTypes(deletedType);
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder.sql("select ")
                .definition(idProp.getStorage(strategy));
        if (readDiscriminator) {
            builder.sql(", ")
                    .sql(discriminatorColumn.name());
        }
        builder
                .sql(" from ")
                .sql(rootType.getTableName(strategy))
                .sql(" where ");
        ComparisonPredicates.renderIn(
                false,
                ValueGetter.valueGetters(sqlClient, idProp),
                ids,
                builder
        );
        renderDiscriminatorPredicate(builder, rootType, deletedType);
        builder.sql(" order by ")
                .definition(idProp.getStorage(strategy));
        sqlClient.getDialect().renderForUpdate(builder, new ForUpdate(LockMode.UPDATE, LockWait.DEFAULT));

        LockedJoinedRows lockedRows = executeJoinedLockQuery(
                sqlClient,
                con,
                builder,
                exceptionTranslator,
                idProp,
                readDiscriminator,
                discriminatorColumn,
                inheritanceInfo.getDiscriminatorTypeMap(),
                rootType
        );
        if (!readDiscriminator && !lockedRows.ids.isEmpty()) {
            for (ImmutableType tableType : joinedTableTypes(rootType, deletedType)) {
                lockedRows.tableIdMap.put(tableType, lockedRows.ids);
            }
        }
        if (lockedRows.tableIdMap.isEmpty()) {
            return lockedRows;
        }
        Map<ImmutableType, Set<Object>> orderedTableIdMap = new LinkedHashMap<>();
        List<ImmutableType> tableTypes = new ArrayList<>(lockedRows.tableIdMap.keySet());
        tableTypes.sort((a, b) -> compareJoinedCleanupTableTypes(strategy, a, b));
        for (ImmutableType tableType : tableTypes) {
            orderedTableIdMap.put(tableType, lockedRows.tableIdMap.get(tableType));
        }
        return new LockedJoinedRows(lockedRows.ids, orderedTableIdMap);
    }

    private static boolean hasEntityDerivedTypes(ImmutableType type) {
        for (ImmutableType derivedType : type.getAllDerivedTypes()) {
            if (derivedType.isEntity()) {
                return true;
            }
        }
        return false;
    }

    private static void deleteJoinedSubtypeTables(
            JSqlClientImplementor sqlClient,
            Connection con,
            Map<ImmutableType, Set<Object>> tableIdMap,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        for (Map.Entry<ImmutableType, Set<Object>> e : tableIdMap.entrySet()) {
            ImmutableType tableType = e.getKey();
            SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
            builder.sql("delete from ")
                    .sql(tableType.getTableName(sqlClient.getMetadataStrategy()))
                    .sql(" where ");
            ComparisonPredicates.renderIn(
                    false,
                    ValueGetter.valueGetters(sqlClient, tableType.getIdProp()),
                    e.getValue(),
                    builder
            );
            execute(sqlClient, con, builder, exceptionTranslator);
        }
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

    private static LockedJoinedRows executeJoinedLockQuery(
            JSqlClientImplementor sqlClient,
            Connection con,
            SqlBuilder builder,
            ExceptionTranslator<?> exceptionTranslator,
            ImmutableProp idProp,
            boolean readDiscriminator,
            DiscriminatorColumn discriminatorColumn,
            Map<String, ImmutableType> discriminatorTypeMap,
            ImmutableType rootType
    ) {
        Tuple3<String, List<Object>, List<Integer>> tuple = builder.build();
        Reader<?> idReader = sqlClient.getReader(idProp);
        return sqlClient.getExecutor().execute(
                new Executor.Args<>(
                        sqlClient,
                        con,
                        tuple.get_1(),
                        tuple.get_2(),
                        tuple.get_3(),
                        ExecutionPurpose.command(QueryReason.LOCK_ID_FOR_JOINED_INHERITANCE_DELETE),
                        exceptionTranslator,
                        null,
                        (stmt, args) -> {
                            Set<Object> lockedIds = new LinkedHashSet<>();
                            Map<ImmutableType, Set<Object>> tableIdMap = new LinkedHashMap<>();
                            Reader.Context readerContext = new Reader.Context(null, sqlClient);
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    readerContext.resetCol();
                                    Object id = idReader.read(rs, readerContext);
                                    lockedIds.add(id);
                                    if (!readDiscriminator) {
                                        continue;
                                    }
                                    String discriminator = rs.getString(readerContext.col());
                                    ImmutableType concreteType = discriminatorTypeMap.get(discriminator);
                                    if (concreteType == null) {
                                        throw new ExecutionException(
                                                "Cannot physically delete joined inheritance rows explicitly, " +
                                                        "the discriminator value \"" +
                                                        discriminator +
                                                        "\" of column \"" +
                                                        discriminatorColumn.name() +
                                                        "\" is not mapped by \"" +
                                                        rootType +
                                                        "\""
                                        );
                                    }
                                    for (ImmutableType tableType : joinedTableTypes(rootType, concreteType)) {
                                        tableIdMap
                                                .computeIfAbsent(tableType, it -> new LinkedHashSet<>())
                                                .add(id);
                                    }
                                }
                            }
                            return new LockedJoinedRows(lockedIds, tableIdMap);
                        }
                )
        );
    }

    private static List<ImmutableType> joinedTableTypes(ImmutableType rootType, ImmutableType type) {
        List<ImmutableType> tableTypes = new ArrayList<>();
        for (ImmutableType t = type; t != rootType; t = t.getPrimarySuperType()) {
            if (t.isEntity()) {
                tableTypes.add(t);
            }
        }
        return tableTypes;
    }

    private static void renderDiscriminatorPredicate(
            SqlBuilder builder,
            ImmutableType tableType,
            ImmutableType deletedType
    ) {
        if (deletedType == tableType) {
            return;
        }
        InheritanceInfo inheritanceInfo = tableType.getInheritanceInfo();
        if (inheritanceInfo == null || inheritanceInfo.getRootType() != tableType) {
            return;
        }
        DiscriminatorColumn column = inheritanceInfo.getDiscriminatorColumn();
        if (column == null) {
            return;
        }
        List<Object> values = new ArrayList<>();
        for (ImmutableType type : deletedTypes(deletedType)) {
            String value = type.getDiscriminatorValue();
            if (value != null) {
                values.add(value);
            }
        }
        if (values.isEmpty()) {
            return;
        }
        builder.sql(" and ")
                .sql(column.name());
        if (values.size() == 1) {
            builder.sql(" = ")
                    .variable(values.get(0));
        } else {
            builder.sql(" in ");
            builder.enter(SqlBuilder.ScopeType.LIST);
            for (Object value : values) {
                builder.separator().variable(value);
            }
            builder.leave();
        }
    }

    private static Collection<ImmutableType> deletedTypes(ImmutableType type) {
        Set<ImmutableType> types = new LinkedHashSet<>();
        types.add(type);
        types.addAll(type.getAllDerivedTypes());
        return types;
    }

    private static int execute(
            JSqlClientImplementor sqlClient,
            Connection con,
            SqlBuilder builder,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        Tuple3<String, List<Object>, List<Integer>> tuple = builder.build();
        Executor.Args<Integer> args = new Executor.Args<>(
                sqlClient,
                con,
                tuple.get_1(),
                tuple.get_2(),
                tuple.get_3(),
                ExecutionPurpose.command(QueryReason.NONE),
                exceptionTranslator,
                null,
                (stmt, a) -> stmt.executeUpdate()
        );
        return sqlClient.getExecutor().execute(args);
    }

    static void fireEvent(
            ImmutableSpi row,
            ImmutableProp prop,
            Object value,
            MutationTrigger trigger
    ) {
        if (prop != null) {
            ImmutableSpi newRow = (ImmutableSpi) Internal.produce(row.__type(), row, draft -> {
                ((DraftSpi) draft).__set(prop.getId(), value);
            });
            trigger.modifyEntityTable(row, newRow);
        } else {
            trigger.modifyEntityTable(row, null);
        }
    }

    private SaveOptions saveOptions() {
        DeleteOptions options = ctx.options;
        return new SaveOptions() {
            @Override
            public JSqlClientImplementor getSqlClient() {
                return options.getSqlClient();
            }

            @Override
            public Connection getConnection() {
                return options.getConnection();
            }

            @Override
            public SaveMode getMode() {
                return SaveMode.UPSERT;
            }

            @Override
            public AssociatedSaveMode getAssociatedMode(ImmutableProp prop) {
                return AssociatedSaveMode.REPLACE;
            }

            @Override
            public Triggers getTriggers() {
                return options.getTriggers();
            }

            @Override
            public KeyMatcher getKeyMatcher(ImmutableType type) {
                return KeyMatcher.EMPTY;
            }

            @Nullable
            @Override
            public UpsertMask<?> getUpsertMask(ImmutableType type) {
                return null;
            }

            @Override
            public boolean isTargetTransferable(ImmutableProp prop) {
                return false;
            }

            @Override
            public DeleteMode getDeleteMode() {
                return options.getMode();
            }

            @Override
            public int getMaxCommandJoinCount() {
                return options.getMaxCommandJoinCount();
            }

            @Override
            public DissociateAction getDissociateAction(ImmutableProp prop) {
                return options.getDissociateAction(prop);
            }

            @Override
            public boolean isPessimisticLocked(ImmutableType type) {
                return false;
            }

            @Override
            public UnloadedVersionBehavior getUnloadedVersionBehavior(ImmutableType type) {
                return UnloadedVersionBehavior.IGNORE;
            }

            @Override
            public UserOptimisticLock<?, ?> getUserOptimisticLock(ImmutableType type) {
                return null;
            }

            @Override
            public boolean isAutoCheckingProp(ImmutableProp prop) {
                return false;
            }

            @Override
            public boolean isIdOnlyAsReference(ImmutableProp prop) {
                return true;
            }

            @Override
            public boolean isKeyOnlyAsReference(ImmutableProp prop) {
                return false;
            }

            @Override
            public boolean isBatchForbidden() {
                return false;
            }

            @Override
            public boolean isConstraintViolationTranslatable() {
                return getSqlClient().isConstraintViolationTranslatable();
            }

            @Override
            public @Nullable ExceptionTranslator<Exception> getExceptionTranslator() {
                return options.getSqlClient().getExceptionTranslator();
            }

            @Override
            public boolean isTransactionRequired() {
                return options.isTransactionRequired();
            }

            @Override
            public boolean isDissociationLogicalDeleteEnabled() {
                return false;
            }
        };
    }

    private static class LockedJoinedRows {

        final Set<Object> ids;

        final Map<ImmutableType, Set<Object>> tableIdMap;

        LockedJoinedRows(Set<Object> ids, Map<ImmutableType, Set<Object>> tableIdMap) {
            this.ids = ids;
            this.tableIdMap = tableIdMap;
        }
    }
}
