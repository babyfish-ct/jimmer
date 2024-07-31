package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.mutation.DeleteOptions;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.DeleteResult;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

public class Deleter {

    private final DeleteContext ctx;

    private Set<Object> ids;

    Deleter(
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
                true,
                affectedRowCountMap,
                MutationPath.root(type)
        );
    }

    public void addIds(Collection<Object> ids) {
        Set<Object> set = this.ids;
        if (set == null) {
            this.ids = set = new LinkedHashSet<>((ids.size() * 4 + 2) / 3);
        }
        Class<?> idType = ctx.path.getType().getIdProp().getReturnClass();
        for (Object id : ids) {
            if (id == null) {
                continue;
            }
            if (id.getClass() != idType) {
                throw new IllegalArgumentException(
                        "Illegal id \"" +
                                id +
                                "\", the expected id type is \"" +
                                idType.getName() +
                                "\" but the actual id type is \"" +
                                id.getClass().getName() +
                                "\""
                );
            }
            set.add(id);
        }
    }

    public DeleteResult execute() {
        Set<Object> ids = this.ids;
        if (ids == null) {
            return new DeleteResult(Collections.emptyMap());
        }
        this.ids = null;

        List<ChildTableOperator> subOperators = AbstractOperator.createSubOperators(
                ctx.options.getSqlClient(),
                ctx.path,
                DisconnectingType.PHYSICAL_DELETE,
                null
        );
        for (ChildTableOperator subOperator : subOperators) {
            subOperator.disconnect(ids);
        }
        List<MiddleTableOperator> middleOperators = AbstractOperator.createMiddleTableOperators(
                ctx.options.getSqlClient(),
                ctx.path,
                DisconnectingType.PHYSICAL_DELETE,
                null
        );
        for (MiddleTableOperator middleTableOperator : middleOperators) {
            middleTableOperator.disconnect(ids);
        }

        int rowCount = executeImpl();
        if (ctx.trigger != null) {
            ctx.trigger.submit(ctx.options.getSqlClient(), ctx.con);
        }

        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
        return new DeleteResult(ctx.affectedRowCountMap);
    }

    private int executeImpl() {
        LogicalDeletedInfo info = ctx.path.getType().getLogicalDeletedInfo();
        boolean logicalDeleted;
        switch (ctx.options.getMode()) {
            case LOGICAL:
                if (info == null) {
                    throw new IllegalArgumentException(
                            "Cannot logically delete the object whose type is \"" +
                                    ctx.path.getType() +
                                    "\" because that type does not support logical deletion"
                    );
                }
                logicalDeleted = true;
                break;
            case PHYSICAL:
                logicalDeleted = false;
                break;
            default:
                logicalDeleted = info != null;
                break;
        }
        return delete(
                ctx.options.getSqlClient(),
                ctx.con,
                ctx.path.getType(),
                ids,
                ctx.trigger,
                logicalDeleted
        );
    }

    static int delete(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Collection<Object> ids,
            MutationTrigger trigger,
            boolean logicalDeleted
    ) {
        LogicalDeletedInfo info = logicalDeleted ? type.getLogicalDeletedInfo() : null;
        if (trigger != null) {
            return deleteWithTrigger(
                    sqlClient,
                    con,
                    type,
                    ids,
                    trigger,
                    info
            );
        }
        return deleteWithoutTrigger(
                sqlClient,
                con,
                type,
                ids,
                info,
                info != null ? info.generateValue() : null
        );
    }

    @SuppressWarnings("unchecked")
    private static int deleteWithTrigger(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType type,
            Collection<Object> ids,
            MutationTrigger trigger,
            LogicalDeletedInfo info
    ) {
        Map<Object, ImmutableSpi> rowMap = (Map<Object, ImmutableSpi>)
                sqlClient
                .caches(CacheDisableConfig::disableAll)
                .findMapByIds(type.getJavaClass(), ids);
        Object generatedDeletedValue = info != null ? info.generateValue() : null;
        Iterator<Object> idItr = ids.iterator();
        while (idItr.hasNext()) {
            Object id = idItr.next();
            ImmutableSpi oldRow = rowMap.get(id);
            if (oldRow == null) {
                idItr.remove();
            } else if (info != null) {
                ImmutableSpi newRow = (ImmutableSpi) Internal.produce(type, oldRow, draft -> {
                    ((DraftSpi)draft).__set(info.getProp().getId(), generatedDeletedValue);
                });
                trigger.modifyEntityTable(oldRow, newRow);
            }
        }
        if (ids.isEmpty()) {
            return 0;
        }
        return deleteWithoutTrigger(sqlClient, con, type, ids, info, generatedDeletedValue);
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
                    .sql(info.getColumnName())
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
                ExecutionPurpose.DELETE,
                null,
                PreparedStatement::executeUpdate
        );
        return sqlClient.getExecutor().execute(args);
    }
}
