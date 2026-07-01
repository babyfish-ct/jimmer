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
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
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
        InheritanceInfo inheritanceInfo = ctx.path.getType().getInheritanceInfo();
        Collection<ImmutableType> deletedTypes = InheritanceMutationUtils.deletedTypes(
                inheritanceInfo,
                ctx.path.getType(),
                ctx.options.getTypeMatchMode()
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
        AcceptedTargets acceptedTargets = null;
        if (inheritanceInfo != null && (
                !cleanup.isEmpty() ||
                        isJoinedTypeBranchCleanupRequired(inheritanceInfo, deletedTypes)
        )) {
            acceptedTargets = resolveAcceptedTargets(ids, rowMap, inheritanceInfo, deletedTypes);
            Set<Object> acceptedIds = acceptedTargets.ids();
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
            if (acceptedTargets != null) {
                disconnectAcceptedTargets(acceptedTargets, inheritanceInfo);
            } else {
                cleanup.disconnect(ids);
            }
        }

        int rowCount = ids.isEmpty() ?
                0 :
                executeImpl(
                        ids,
                        rowMap,
                        acceptedTargets != null ?
                                acceptedTargets.joinedTypeBranchTableIdMap(
                                        inheritanceInfo.getRootType(),
                                        ctx.options.getSqlClient().getMetadataStrategy()
                                ) :
                                null,
                        ctx.getOptions().getExceptionTranslator()
                );
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
                TypeMatchModes.isPolymorphic(ctx.path.getType(), ctx.options.getTypeMatchMode()) ||
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
        List<ImmutableType> stageTypes = InheritanceMutationUtils.joinedTableTypes(rootType, ctx.path.getType());
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
                            "\" because a parent stage was not accepted after a type-branch stage had been deleted"
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
        if (!TypeMatchModes.isPolymorphic(ctx.path.getType(), ctx.options.getTypeMatchMode()) ||
                ctx.path.getParent() != null) {
            return Collections.singletonList(ctx);
        }
        InheritanceInfo inheritanceInfo = ctx.path.getType().getInheritanceInfo();
        if (inheritanceInfo == null) {
            return Collections.singletonList(ctx);
        }
        Collection<ImmutableType> deletedTypes = InheritanceMutationUtils.deletedTypes(
                inheritanceInfo,
                ctx.path.getType(),
                TypeMatchMode.POLYMORPHIC
        );
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
                            prop -> !declaredOnly || TableImplementor.isPropOwnedByStage(prop, cleanupCtx.path.getType()),
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

    private static boolean isBackPropOwnedByStage(ImmutableProp backProp, ImmutableType stageType) {
        return backProp.getTargetType() == stageType;
    }

    private boolean isJoinedTypeBranchCleanupRequired(
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
            if (!InheritanceMutationUtils.joinedTableTypes(rootType, deletedType).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private AcceptedTargets resolveAcceptedTargets(
            Collection<Object> ids,
            @Nullable Map<Object, ImmutableSpi> rowMap,
            InheritanceInfo inheritanceInfo,
            Collection<ImmutableType> deletedTypes
    ) {
        if (ids.isEmpty()) {
            return AcceptedTargets.EMPTY;
        }
        if (rowMap != null) {
            return AcceptedTargets.ofRows(rowMap, deletedTypes);
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
        PropExpression<Object> discriminatorExpr = table.get(inheritanceInfo.getDiscriminatorProp().getName());
        ConfigurableRootQuery<?, Tuple2<Object, Object>> acceptedTargetQuery = query.select(idExpr, discriminatorExpr);
        Map<Object, ImmutableType> discriminatorTypeMap = inheritanceInfo.getDiscriminatorTypeMap();
        Map<Object, ImmutableType> typeById = new LinkedHashMap<>();
        for (Tuple2<Object, Object> tuple : acceptedTargetQuery.execute(ctx.con)) {
            ImmutableType type = discriminatorTypeMap.get(tuple.get_2());
            if (type == null) {
                throw new ExecutionException(
                        "Cannot delete inheritance rows, the discriminator value \"" +
                                tuple.get_2() +
                                "\" is not mapped by \"" +
                                rootType +
                                "\""
                );
            }
            typeById.put(tuple.get_1(), type);
        }
        return AcceptedTargets.ofTypeById(ids, typeById, deletedTypes);
    }

    private void disconnectAcceptedTargets(AcceptedTargets acceptedTargets, InheritanceInfo inheritanceInfo) {
        Set<Object> ids = acceptedTargets.ids();
        if (ids.isEmpty()) {
            return;
        }
        ImmutableType rootType = inheritanceInfo.getRootType();
        associationCleanup(rootType, true).disconnect(ids);
        Map<ImmutableType, Set<Object>> stageIdMap = acceptedTargets.joinedTypeBranchTableIdMap(
                rootType,
                ctx.options.getSqlClient().getMetadataStrategy()
        );
        if (stageIdMap.isEmpty()) {
            stageIdMap = acceptedTargets.typeIdMapForNonRoot(rootType);
        }
        for (Map.Entry<ImmutableType, Set<Object>> e : stageIdMap.entrySet()) {
            associationCleanup(e.getKey(), true).disconnect(e.getValue());
        }
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

    private static class AcceptedTargets {

        static final AcceptedTargets EMPTY = new AcceptedTargets(Collections.emptyMap());

        private final Map<ImmutableType, Set<Object>> typeIdMap;

        private AcceptedTargets(Map<ImmutableType, Set<Object>> typeIdMap) {
            this.typeIdMap = typeIdMap;
        }

        static AcceptedTargets ofRows(
                Map<Object, ImmutableSpi> rowMap,
                Collection<ImmutableType> deletedTypes
        ) {
            Set<ImmutableType> deletedTypeSet = new HashSet<>(deletedTypes);
            Map<ImmutableType, Set<Object>> typeIdMap = new LinkedHashMap<>();
            for (Map.Entry<Object, ImmutableSpi> e : rowMap.entrySet()) {
                ImmutableType type = e.getValue().__type();
                if (deletedTypeSet.contains(type)) {
                    typeIdMap.computeIfAbsent(type, it -> new LinkedHashSet<>()).add(e.getKey());
                }
            }
            return typeIdMap.isEmpty() ? EMPTY : new AcceptedTargets(typeIdMap);
        }

        static AcceptedTargets ofTypeById(
                Collection<Object> ids,
                Map<Object, ImmutableType> typeById,
                Collection<ImmutableType> deletedTypes
        ) {
            Set<ImmutableType> deletedTypeSet = new HashSet<>(deletedTypes);
            Map<ImmutableType, Set<Object>> typeIdMap = new LinkedHashMap<>();
            for (Object id : ids) {
                ImmutableType type = typeById.get(id);
                if (type != null && deletedTypeSet.contains(type)) {
                    typeIdMap.computeIfAbsent(type, it -> new LinkedHashSet<>()).add(id);
                }
            }
            return typeIdMap.isEmpty() ? EMPTY : new AcceptedTargets(typeIdMap);
        }

        Set<Object> ids() {
            if (typeIdMap.isEmpty()) {
                return Collections.emptySet();
            }
            Set<Object> ids = new LinkedHashSet<>();
            for (Set<Object> typeIds : typeIdMap.values()) {
                ids.addAll(typeIds);
            }
            return ids;
        }

        Map<ImmutableType, Set<Object>> joinedTypeBranchTableIdMap(
                ImmutableType rootType,
                org.babyfish.jimmer.sql.meta.MetadataStrategy strategy
        ) {
            Map<ImmutableType, Set<Object>> tableIdMap = new LinkedHashMap<>();
            for (Map.Entry<ImmutableType, Set<Object>> e : typeIdMap.entrySet()) {
                for (ImmutableType tableType : InheritanceMutationUtils.joinedTableTypes(rootType, e.getKey())) {
                    tableIdMap.computeIfAbsent(tableType, it -> new LinkedHashSet<>()).addAll(e.getValue());
                }
            }
            if (tableIdMap.size() > 1) {
                List<Map.Entry<ImmutableType, Set<Object>>> entries = new ArrayList<>(tableIdMap.entrySet());
                entries.sort(Map.Entry.comparingByKey(InheritanceMutationUtils.joinedCleanupTableTypeComparator(strategy)));
                tableIdMap = new LinkedHashMap<>();
                for (Map.Entry<ImmutableType, Set<Object>> e : entries) {
                    tableIdMap.put(e.getKey(), e.getValue());
                }
            }
            return tableIdMap;
        }

        Map<ImmutableType, Set<Object>> typeIdMapForNonRoot(ImmutableType rootType) {
            Map<ImmutableType, Set<Object>> map = new LinkedHashMap<>();
            for (Map.Entry<ImmutableType, Set<Object>> e : typeIdMap.entrySet()) {
                if (e.getKey() != rootType) {
                    map.put(e.getKey(), e.getValue());
                }
            }
            return map;
        }
    }

    private int executeImpl(
            Collection<Object> ids,
            Map<Object, ImmutableSpi> rowMap,
            @Nullable Map<ImmutableType, Set<Object>> joinedTypeBranchTableIdMap,
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
                ctx.options.getTypeMatchMode(),
                joinedTypeBranchTableIdMap,
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
            TypeMatchMode typeMatchMode,
            @Nullable Map<ImmutableType, Set<Object>> joinedTypeBranchTableIdMap,
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
                    typeMatchMode,
                    joinedTypeBranchTableIdMap,
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
                typeMatchMode,
                joinedTypeBranchTableIdMap,
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
            TypeMatchMode typeMatchMode,
            @Nullable Map<ImmutableType, Set<Object>> joinedTypeBranchTableIdMap,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        Collection<ImmutableType> deletedTypes = InheritanceMutationUtils.deletedTypes(inheritanceInfo, type, typeMatchMode);
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
        return deleteWithoutTrigger(
                sqlClient,
                con,
                type,
                rowMap.keySet(),
                info,
                generatedValue,
                typeMatchMode,
                joinedTypeBranchTableIdMap,
                exceptionTranslator
        );
    }

    private static int deleteWithoutTrigger(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Collection<Object> ids,
            LogicalDeletedInfo info,
            Object generatedDeletedValue,
            TypeMatchMode typeMatchMode,
            @Nullable Map<ImmutableType, Set<Object>> joinedTypeBranchTableIdMap,
            ExceptionTranslator<?> exceptionTranslator
    ) {
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo != null) {
            ImmutableType rootType = inheritanceInfo.getRootType();
            Collection<ImmutableType> deletedTypes = InheritanceMutationUtils.deletedTypes(inheritanceInfo, type, typeMatchMode);
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
                deleteJoinedTypeBranchTables(
                        sqlClient,
                        con,
                        joinedTypeBranchTableIdMap != null ?
                                joinedTypeBranchTableIdMap :
                                joinedTypeBranchTableIdMap(
                                        rootType,
                                        deletedTypes,
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
        InheritanceMutationUtils.renderDiscriminatorPredicate(builder, tableType, deletedTypes);
        return execute(sqlClient, con, builder, exceptionTranslator);
    }

    private static void deleteJoinedTypeBranchTables(
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

    private static Map<ImmutableType, Set<Object>> joinedTypeBranchTableIdMap(
            ImmutableType rootType,
            Collection<ImmutableType> deletedTypes,
            Collection<Object> ids,
            org.babyfish.jimmer.sql.meta.MetadataStrategy strategy
    ) {
        Set<Object> idSet = ids instanceof Set<?> ?
                (Set<Object>) ids :
                new LinkedHashSet<>(ids);
        Map<ImmutableType, Set<Object>> tableIdMap = new LinkedHashMap<>();
        Set<ImmutableType> tableTypeSet = new LinkedHashSet<>();
        for (ImmutableType deletedType : deletedTypes) {
            tableTypeSet.addAll(InheritanceMutationUtils.joinedTableTypes(rootType, deletedType));
        }
        List<ImmutableType> tableTypes = new ArrayList<>(tableTypeSet);
        tableTypes.sort(InheritanceMutationUtils.joinedCleanupTableTypeComparator(strategy));
        for (ImmutableType tableType : tableTypes) {
            tableIdMap.put(tableType, idSet);
        }
        return tableIdMap;
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
        List<Object> values = InheritanceMutationUtils.discriminatorValues(inheritanceInfo, deletedTypes);
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
            public TypeMatchMode getTypeMatchMode() {
                return TypeMatchMode.AUTO;
            }

            @Override
            public TypeMatchMode getAssociatedTypeMatchMode(ImmutableProp prop, ImmutableType targetType) {
                return TypeMatchMode.AUTO;
            }

            @Override
            public boolean isTypeChangeAllowed() {
                return false;
            }

            @Override
            public boolean isAssociatedTypeChangeAllowed(ImmutableProp prop, ImmutableType targetType) {
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
