package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.JoinedTableDissociateAction;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.Variables;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
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
        validateInheritanceDeleteTarget();

        InheritanceInfo inheritanceInfo = ctx.path.getType().getInheritanceInfo();
        Collection<ImmutableType> deletedTypes = deletedTypes(
                inheritanceInfo,
                ctx.path.getType(),
                ctx.options.isPolymorphic()
        );
        if (isStagedJoinedExactDeleteAvailable(inheritanceInfo, deletedTypes)) {
            int rowCount = executeStagedJoinedExactDelete(ids);
            if (ctx.trigger != null) {
                ctx.trigger.submit(ctx.options.getSqlClient(), ctx.con);
            }
            AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
            return new DeleteResult(ctx.affectedRowCountMap);
        }

        AssociationCleanup cleanup = associationCleanup();
        if (inheritanceInfo != null && (
                !cleanup.isEmpty() ||
                        isJoinedSubtypeCleanupRequired(inheritanceInfo, deletedTypes)
        )) {
            Set<Object> acceptedIds = resolveAcceptedTargetIds(ids, inheritanceInfo, deletedTypes);
            if (acceptedIds.isEmpty()) {
                ids = Collections.emptySet();
                rowMap = null;
            } else {
                ids = acceptedIds;
                if (rowMap != null) {
                    rowMap = filterRowMap(rowMap, acceptedIds);
                }
            }
        }
        if (!ids.isEmpty()) {
            cleanup.disconnect(ids);
        }

        int rowCount = ids.isEmpty() ?
                0 :
                executeImpl(ids, rowMap, ctx.getOptions().getExceptionTranslator());
        if (ctx.trigger != null) {
            ctx.trigger.submit(ctx.options.getSqlClient(), ctx.con);
        }

        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
        return new DeleteResult(ctx.affectedRowCountMap);
    }

    private boolean isStagedJoinedExactDeleteAvailable(
            @Nullable InheritanceInfo inheritanceInfo,
            Collection<ImmutableType> deletedTypes
    ) {
        if (inheritanceInfo == null ||
                ctx.isLogicalDeleted() ||
                ctx.options.isPolymorphic() ||
                ctx.trigger != null ||
                inheritanceInfo.getStrategy() != InheritanceType.JOINED ||
                inheritanceInfo.getJoinedTableDissociateAction() != JoinedTableDissociateAction.DELETE ||
                deletedTypes.size() != 1) {
            return false;
        }
        ImmutableType type = deletedTypes.iterator().next();
        return type != inheritanceInfo.getRootType() && type.getAllDerivedTypes().isEmpty();
    }

    private int executeStagedJoinedExactDelete(Collection<Object> ids) {
        InheritanceInfo inheritanceInfo = ctx.path.getType().getInheritanceInfo();
        ImmutableType rootType = inheritanceInfo.getRootType();
        List<ImmutableType> stageTypes = joinedTableTypes(rootType, ctx.path.getType());
        Collection<Object> acceptedIds = ids;
        for (ImmutableType stageType : stageTypes) {
            associationCleanup(stageType, true).disconnect(acceptedIds);
            acceptedIds = deleteJoinedStageRows(stageType, acceptedIds);
            if (acceptedIds.isEmpty()) {
                return 0;
            }
        }
        associationCleanup(rootType, true).disconnect(acceptedIds);
        int rowCount = deleteFromSingleTable(
                ctx.options.getSqlClient(),
                ctx.con,
                rootType,
                Collections.singleton(ctx.path.getType()),
                acceptedIds,
                null,
                null,
                ctx.options.getExceptionTranslator()
        );
        if (rowCount != acceptedIds.size()) {
            throw new ExecutionException(
                    "Cannot complete staged joined inheritance delete for \"" +
                            ctx.path.getType() +
                            "\" because a parent stage was not accepted after a subtype stage had been deleted"
            );
        }
        return rowCount;
    }

    private Set<Object> deleteJoinedStageRows(ImmutableType stageType, Collection<Object> ids) {
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }
        if (ids.size() == 1 ||
                ctx.options.isBatchForbidden() ||
                ctx.options.getSqlClient().getDialect().isBatchDumb() ||
                ValueGetter.valueGetters(ctx.options.getSqlClient(), stageType.getIdProp()).size() != 1) {
            Set<Object> acceptedIds = new LinkedHashSet<>();
            for (Object id : ids) {
                int rowCount = deleteJoinedStageRowsOneByOne(stageType, Collections.singleton(id));
                if (rowCount != 0) {
                    acceptedIds.add(id);
                }
            }
            return acceptedIds;
        }
        return deleteJoinedStageRowsByBatch(stageType, ids);
    }

    private int deleteJoinedStageRowsOneByOne(ImmutableType stageType, Collection<Object> ids) {
        SqlBuilder builder = new SqlBuilder(new AstContext(ctx.options.getSqlClient()));
        builder.sql("delete from ")
                .sql(stageType.getTableName(ctx.options.getSqlClient().getMetadataStrategy()))
                .sql(" where ");
        ComparisonPredicates.renderIn(
                false,
                ValueGetter.valueGetters(ctx.options.getSqlClient(), stageType.getIdProp()),
                ids,
                builder
        );
        return execute(ctx.options.getSqlClient(), ctx.con, builder, ctx.options.getExceptionTranslator());
    }

    @SuppressWarnings("unchecked")
    private Set<Object> deleteJoinedStageRowsByBatch(ImmutableType stageType, Collection<Object> ids) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        List<ValueGetter> idGetters = ValueGetter.valueGetters(sqlClient, stageType.getIdProp());
        builder.sql("delete from ")
                .sql(stageType.getTableName(sqlClient.getMetadataStrategy()))
                .sql(" where ");
        builder.enter(BatchSqlBuilder.ScopeType.AND);
        for (ValueGetter idGetter : idGetters) {
            builder.separator()
                    .sql(idGetter)
                    .sql(" = ")
                    .variable(idGetter);
        }
        builder.leave();
        Tuple3<String, BatchSqlBuilder.VariableMapper, List<Integer>> tuple = builder.build();
        int[] rowCounts;
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor()
                .executeBatch(
                        ctx.con,
                        tuple.get_1(),
                        null,
                        ExecutionPurpose.command(QueryReason.NONE),
                        sqlClient,
                        sqlClient.isConstraintViolationTranslatable()
                )
        ) {
            for (Object id : ids) {
                batchContext.add(tuple.get_2().variables(id));
            }
            rowCounts = batchContext.execute((ex, args) -> {
                ExceptionTranslator<?> exceptionTranslator = ctx.options.getExceptionTranslator();
                if (exceptionTranslator != null) {
                    return ((ExceptionTranslator<Exception>) exceptionTranslator).translate(ex, args);
                }
                return ex;
            });
        }
        Set<Object> acceptedIds = new LinkedHashSet<>();
        int index = 0;
        for (Object id : ids) {
            if (index < rowCounts.length && rowCounts[index++] != 0) {
                acceptedIds.add(id);
            }
        }
        return acceptedIds;
    }

    private List<DeleteContext> associationCleanupContexts() {
        if (!ctx.options.isPolymorphic() || ctx.path.getParent() != null) {
            return Collections.singletonList(ctx);
        }
        InheritanceInfo inheritanceInfo = ctx.path.getType().getInheritanceInfo();
        if (inheritanceInfo == null) {
            return Collections.singletonList(ctx);
        }
        Collection<ImmutableType> deletedTypes = deletedTypes(inheritanceInfo, ctx.path.getType(), true);
        if (deletedTypes.size() == 1 && deletedTypes.iterator().next() == ctx.path.getType()) {
            return Collections.singletonList(ctx);
        }
        List<DeleteContext> contexts = new ArrayList<>(deletedTypes.size() + 1);
        contexts.add(ctx);
        for (ImmutableType deletedType : deletedTypes) {
            if (deletedType != ctx.path.getType()) {
                contexts.add(
                        new DeleteContext(
                                ctx.options,
                                ctx.con,
                                ctx.trigger,
                                ctx.affectedRowCountMap,
                                MutationPath.root(deletedType)
                        )
                );
            }
        }
        return contexts;
    }

    private AssociationCleanup associationCleanup() {
        return associationCleanup(null, false);
    }

    private AssociationCleanup associationCleanup(@Nullable ImmutableType stageType, boolean declaredOnly) {
        DisconnectingType disconnectingType = ctx.isLogicalDeleted() ?
                DisconnectingType.LOGICAL_DELETE :
                DisconnectingType.PHYSICAL_DELETE;
        List<MiddleTableOperator> middleOperators = new ArrayList<>();
        List<ChildTableOperator> childOperators = new ArrayList<>();
        Set<ImmutableProp> handledMiddleProps = new HashSet<>();
        Set<ImmutableProp> handledMiddleBackProps = new HashSet<>();
        Set<ImmutableProp> handledChildBackProps = new HashSet<>();
        for (DeleteContext cleanupCtx : associationCleanupContexts(stageType)) {
            SaveContext saveCtx = new SaveContext(
                    saveOptions(),
                    cleanupCtx.con,
                    cleanupCtx.path.getType(),
                    null,
                    cleanupCtx.trigger,
                    cleanupCtx.affectedRowCountMap
            );
            middleOperators.addAll(
                    AbstractAssociationOperator.createMiddleTableOperators(
                            cleanupCtx.options.getSqlClient(),
                            cleanupCtx.path,
                            disconnectingType,
                            prop -> handledMiddleProps.add(prop) ?
                                    new MiddleTableOperator(saveCtx.prop(prop), cleanupCtx.isLogicalDeleted()) :
                                    null,
                            backProp -> handledMiddleBackProps.add(backProp) ?
                                    new MiddleTableOperator(saveCtx.backProp(backProp), cleanupCtx.isLogicalDeleted()) :
                                    null,
                            prop -> !declaredOnly || isPropOwnedByStage(prop, cleanupCtx.path.getType()),
                            backProp -> !declaredOnly || isBackPropOwnedByStage(backProp, cleanupCtx.path.getType())
                    )
            );
            childOperators.addAll(
                    AbstractAssociationOperator.createSubOperators(
                            cleanupCtx.options.getSqlClient(),
                            cleanupCtx.path,
                            disconnectingType,
                            backProp -> handledChildBackProps.add(backProp) ?
                                    new ChildTableOperator(cleanupCtx.backPropOf(backProp), false) :
                                    null,
                            backProp -> !declaredOnly || isBackPropOwnedByStage(backProp, cleanupCtx.path.getType())
                    )
            );
        }
        return new AssociationCleanup(middleOperators, childOperators);
    }

    private List<DeleteContext> associationCleanupContexts(@Nullable ImmutableType stageType) {
        if (stageType != null) {
            return Collections.singletonList(
                    new DeleteContext(
                            ctx.options,
                            ctx.con,
                            ctx.trigger,
                            ctx.affectedRowCountMap,
                            MutationPath.root(stageType)
                    )
            );
        }
        return associationCleanupContexts();
    }

    private static boolean isPropOwnedByStage(ImmutableProp prop, ImmutableType stageType) {
        return isDeclaringTypeOwnedByStage(prop.getDeclaringType(), stageType);
    }

    private static boolean isBackPropOwnedByStage(ImmutableProp backProp, ImmutableType stageType) {
        return backProp.getTargetType() == stageType;
    }

    private static boolean isDeclaringTypeOwnedByStage(ImmutableType declaringType, ImmutableType stageType) {
        if (declaringType == stageType) {
            return true;
        }
        if (!declaringType.isMappedSuperclass() || !stageType.getAllTypes().contains(declaringType)) {
            return false;
        }
        ImmutableType superType = stageType.getPrimarySuperType();
        return superType == null || !superType.getAllTypes().contains(declaringType);
    }

    private void validateInheritanceDeleteTarget() {
        ImmutableType type = ctx.path.getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null) {
            return;
        }
        Collection<ImmutableType> deletedTypes = deletedTypes(inheritanceInfo, type, ctx.options.isPolymorphic());
        if (ctx.isLogicalDeleted() ||
                inheritanceInfo.getStrategy() != InheritanceType.JOINED ||
                inheritanceInfo.getJoinedTableDissociateAction() == JoinedTableDissociateAction.LAX) {
            return;
        }
        if (ctx.options.isPolymorphic() && (deletedTypes.size() != 1 || deletedTypes.iterator().next() != type)) {
            throw new ExecutionException(
                    "Cannot physically delete joined inheritance rows polymorphically by type \"" +
                            type +
                            "\" when joinedTableDissociateAction is \"" +
                            JoinedTableDissociateAction.DELETE +
                            "\". Delete exact concrete subtypes, use joinedTableDissociateAction = " +
                            JoinedTableDissociateAction.LAX +
                            ", or explicitly select concrete rows and delete them as exact concrete subtypes."
            );
        }
    }

    private boolean isJoinedSubtypeCleanupRequired(
            InheritanceInfo inheritanceInfo,
            Collection<ImmutableType> deletedTypes
    ) {
        if (ctx.isLogicalDeleted() ||
                inheritanceInfo.getStrategy() != InheritanceType.JOINED ||
                inheritanceInfo.getJoinedTableDissociateAction() != JoinedTableDissociateAction.DELETE) {
            return false;
        }
        ImmutableType rootType = inheritanceInfo.getRootType();
        for (ImmutableType deletedType : deletedTypes) {
            if (!joinedTableTypes(rootType, deletedType).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Set<Object> resolveAcceptedTargetIds(
            Collection<Object> ids,
            InheritanceInfo inheritanceInfo,
            Collection<ImmutableType> deletedTypes
    ) {
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }
        ImmutableType rootType = inheritanceInfo.getRootType();
        MutableRootQueryImpl<Table<?>> query = new MutableRootQueryImpl<>(
                ctx.options.getSqlClient(),
                rootType,
                ExecutionPurpose.command(QueryReason.RESOLVE_ACCEPTED_INHERITANCE_DELETE_TARGETS),
                FilterLevel.IGNORE_ALL
        );
        Table<ImmutableSpi> table = query.getTable();
        PropExpression<Object> idExpr = table.get(rootType.getIdProp().getName());
        if (ids.size() == 1) {
            query.where(idExpr.eq(ids.iterator().next()));
        } else {
            query.where(idExpr.in(ids));
        }
        addDiscriminatorPredicate(query, table, inheritanceInfo, deletedTypes);
        ConfigurableRootQuery<?, Object> acceptedIdQuery = query.select(idExpr);
        return new LinkedHashSet<>(acceptedIdQuery.execute(ctx.con));
    }

    private static Map<Object, ImmutableSpi> filterRowMap(
            Map<Object, ImmutableSpi> rowMap,
            Set<Object> acceptedIds
    ) {
        Map<Object, ImmutableSpi> filteredMap = new LinkedHashMap<>((acceptedIds.size() * 4 + 2) / 3);
        for (Object id : acceptedIds) {
            ImmutableSpi row = rowMap.get(id);
            if (row != null) {
                filteredMap.put(id, row);
            }
        }
        return filteredMap;
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
                ctx.options.isPolymorphic(),
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
            boolean polymorphic,
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
                    polymorphic,
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
                polymorphic,
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
            boolean polymorphic,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        Collection<ImmutableType> deletedTypes = deletedTypes(inheritanceInfo, type, polymorphic);
        if (rowMap == null) {
            MutableRootQueryImpl<Table<?>> q = new MutableRootQueryImpl<>(
                    sqlClient,
                    type,
                    ExecutionPurpose.command(QueryReason.TRIGGER),
                    info != null ? FilterLevel.IGNORE_USER_FILTERS : FilterLevel.IGNORE_ALL
            );
            Table<ImmutableSpi> t = q.getTable();
            q.where(t.get(type.getIdProp().getName()).in(ids));
            addDiscriminatorPredicate(q, t, inheritanceInfo, deletedTypes);
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
        return deleteWithoutTrigger(sqlClient, con, type, rowMap.keySet(), info, generatedValue, polymorphic, exceptionTranslator);
    }

    private static int deleteWithoutTrigger(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Collection<Object> ids,
            LogicalDeletedInfo info,
            Object generatedDeletedValue,
            boolean polymorphic,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo != null) {
            ImmutableType rootType = inheritanceInfo.getRootType();
            Collection<ImmutableType> deletedTypes = deletedTypes(inheritanceInfo, type, polymorphic);
            if (info != null) {
                return deleteFromSingleTable(
                        sqlClient,
                        con,
                        rootType,
                        deletedTypes,
                        ids,
                        info,
                        generatedDeletedValue,
                        exceptionTranslator
                );
            }
            if (inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                    inheritanceInfo.getJoinedTableDissociateAction() == JoinedTableDissociateAction.DELETE) {
                deleteJoinedSubtypeTables(
                        sqlClient,
                        con,
                        joinedSubtypeTableIdMap(
                                rootType,
                                deletedTypes.iterator().next(),
                                ids,
                                sqlClient.getMetadataStrategy()
                        ),
                        exceptionTranslator
                );
            }
            return deleteFromSingleTable(
                    sqlClient,
                    con,
                    rootType,
                    deletedTypes,
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
                Collections.singleton(type),
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
            Collection<ImmutableType> deletedTypes,
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
        renderDiscriminatorPredicate(builder, tableType, deletedTypes);
        return execute(sqlClient, con, builder, exceptionTranslator);
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

    private static Map<ImmutableType, Set<Object>> joinedSubtypeTableIdMap(
            ImmutableType rootType,
            ImmutableType concreteType,
            Collection<Object> ids,
            MetadataStrategy strategy
    ) {
        Set<Object> idSet = ids instanceof Set<?> ?
                (Set<Object>) ids :
                new LinkedHashSet<>(ids);
        Map<ImmutableType, Set<Object>> tableIdMap = new LinkedHashMap<>();
        List<ImmutableType> tableTypes = joinedTableTypes(rootType, concreteType);
        tableTypes.sort((a, b) -> compareJoinedCleanupTableTypes(strategy, a, b));
        for (ImmutableType tableType : tableTypes) {
            tableIdMap.put(tableType, idSet);
        }
        return tableIdMap;
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
            Collection<ImmutableType> deletedTypes
    ) {
        InheritanceInfo inheritanceInfo = tableType.getInheritanceInfo();
        if (inheritanceInfo == null || inheritanceInfo.getRootType() != tableType) {
            return;
        }
        ImmutableProp discriminatorProp = inheritanceInfo.getDiscriminatorProp();
        List<Object> values = new ArrayList<>();
        for (ImmutableType type : deletedTypes) {
            String value = type.getDiscriminatorValue();
            if (value != null) {
                values.add(inheritanceInfo.discriminatorValue(value));
            }
        }
        if (values.isEmpty()) {
            return;
        }
        builder.sql(" and ")
                .sql(discriminatorProp.<SingleColumn>getStorage(builder.getAstContext().getSqlClient().getMetadataStrategy()).getName());
        if (values.size() == 1) {
            builder.sql(" = ")
                    .variable(Variables.process(values.get(0), discriminatorProp, builder.getAstContext().getSqlClient()));
        } else {
            builder.sql(" in ");
            builder.enter(SqlBuilder.ScopeType.LIST);
            for (Object value : values) {
                builder.separator().variable(Variables.process(value, discriminatorProp, builder.getAstContext().getSqlClient()));
            }
            builder.leave();
        }
    }

    private static Collection<ImmutableType> deletedTypes(
            @Nullable InheritanceInfo inheritanceInfo,
            ImmutableType type,
            boolean polymorphic
    ) {
        if (inheritanceInfo == null) {
            return Collections.singleton(type);
        }
        if (!polymorphic) {
            if (!type.isInstantiable()) {
                throw new ExecutionException(
                        "Cannot delete inheritance entity type \"" +
                                type +
                                "\" exactly because it is abstract. Delete an instantiable subtype or enable polymorphic delete."
                );
            }
            return Collections.singleton(type);
        }
        Collection<ImmutableType> types = inheritanceInfo.getConcreteTypes(type);
        if (types.isEmpty()) {
            throw new ExecutionException(
                    "Cannot delete inheritance entity type \"" +
                            type +
                            "\" polymorphically because it has no instantiable subtype"
            );
        }
        return types;
    }

    private static void addDiscriminatorPredicate(
            MutableRootQueryImpl<Table<?>> query,
            Table<ImmutableSpi> table,
            @Nullable InheritanceInfo inheritanceInfo,
            Collection<ImmutableType> deletedTypes
    ) {
        if (inheritanceInfo == null) {
            return;
        }
        List<Object> values = new ArrayList<>();
        for (ImmutableType type : deletedTypes) {
            String value = type.getDiscriminatorValue();
            if (value != null) {
                values.add(inheritanceInfo.discriminatorValue(value));
            }
        }
        if (values.isEmpty()) {
            return;
        }
        PropExpression<Object> expr = table.get(inheritanceInfo.getDiscriminatorProp().getName());
        query.where(values.size() == 1 ? expr.eq(values.get(0)) : expr.in(values));
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
            public boolean isSubtypeChangeAllowed() {
                return false;
            }

            @Override
            public boolean isAssociatedSubtypeChangeAllowed(ImmutableProp prop, ImmutableType targetType) {
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

    private static class AssociationCleanup {

        private final List<MiddleTableOperator> middleOperators;

        private final List<ChildTableOperator> childOperators;

        AssociationCleanup(
                List<MiddleTableOperator> middleOperators,
                List<ChildTableOperator> childOperators
        ) {
            this.middleOperators = middleOperators;
            this.childOperators = childOperators;
        }

        boolean isEmpty() {
            return middleOperators.isEmpty() && childOperators.isEmpty();
        }

        void disconnect(Collection<Object> ids) {
            for (MiddleTableOperator middleOperator : middleOperators) {
                middleOperator.disconnect(ids);
            }
            for (ChildTableOperator childOperator : childOperators) {
                childOperator.disconnect(ids);
            }
        }
    }

}
