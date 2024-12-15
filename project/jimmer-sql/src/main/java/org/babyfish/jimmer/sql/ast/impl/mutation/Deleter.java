package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.impl.LogicalDeletedValueGenerators;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
                backProp -> new ChildTableOperator(ctx.backPropOf(backProp))
        );
        for (ChildTableOperator subOperator : subOperators) {
            subOperator.disconnect(ids);
        }

        int rowCount = executeImpl(ids, rowMap);
        if (ctx.trigger != null) {
            ctx.trigger.submit(ctx.options.getSqlClient(), ctx.con);
        }

        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
        return new DeleteResult(ctx.affectedRowCountMap);
    }

    private int executeImpl(Collection<Object> ids, Map<Object, ImmutableSpi> rowMap) {
        return delete(
                ctx.options.getSqlClient(),
                ctx.con,
                ctx.path.getType(),
                ids,
                rowMap,
                ctx.trigger,
                ctx.isLogicalDeleted()
        );
    }

    private static int delete(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Collection<Object> ids,
            Map<Object, ImmutableSpi> rowMap,
            MutationTrigger trigger,
            boolean logicalDeleted
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
                    generator != null ? generator.generate() : null
            );
        }
        return deleteWithoutTrigger(
                sqlClient,
                con,
                type,
                ids != null ? ids : rowMap.keySet(),
                info,
                generator != null ? generator.generate() : null
        );
    }

    @SuppressWarnings("unchecked")
    private static int deleteWithTrigger(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Collection<Object> ids,
            Map<Object, ImmutableSpi> rowMap,
            MutationTrigger trigger,
            LogicalDeletedInfo info,
            Object generatedValue
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
        return deleteWithoutTrigger(sqlClient, con, type, rowMap.keySet(), info, generatedValue);
    }

    private static int deleteWithoutTrigger(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Collection<Object> ids,
            LogicalDeletedInfo info,
            Object generatedDeletedValue
    ) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        if (info != null) {
            builder.sql("update ")
                    .sql(type.getTableName(sqlClient.getMetadataStrategy()))
                    .sql(" set ")
                    .sql(info.getProp().<SingleColumn>getStorage(sqlClient.getMetadataStrategy()).getName())
                    .sql(" = ");
            if (generatedDeletedValue != null) {
                builder.variable(generatedDeletedValue);
            } else {
                builder.sql("null");
            }
        } else {
            builder.sql("delete from ")
                    .sql(type.getTableName(sqlClient.getMetadataStrategy()));
        }
        builder.sql(" where ");
        ComparisonPredicates.renderIn(
                false,
                ValueGetter.valueGetters(sqlClient, type.getIdProp()),
                ids,
                builder
        );
        Tuple3<String, List<Object>, List<Integer>> tuple = builder.build();
        Executor.Args<Integer> args = new Executor.Args<>(
                sqlClient,
                con,
                tuple.get_1(),
                tuple.get_2(),
                tuple.get_3(),
                ExecutionPurpose.command(QueryReason.NONE),
                null,
                PreparedStatement::executeUpdate
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
                ((DraftSpi)draft).__set(prop.getId(), value);
            });;
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

            @Override
            public boolean isTargetTransferable(ImmutableProp prop) {
                return false;
            }

            @Override
            public DeleteMode getDeleteMode() {
                return options.getMode();
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
            public boolean isKeyOnlyAsReference(ImmutableProp prop) {
                return false;
            }

            @Override
            public boolean isBatchForbidden() {
                return false;
            }

            @Override
            public @Nullable ExceptionTranslator<Exception> getExceptionTranslator() {
                return null;
            }
        };
    }
}
