package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.mutation.DeleteOptions;
import org.babyfish.jimmer.sql.ast.impl.mutation.SaveOptions;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.impl.LogicalDeletedValueGenerators;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

public class Deleter2 {

    private final DeleteContext ctx;

    private Set<Object> ids;

    public Deleter2(
            ImmutableType type,
            DeleteOptions options,
            Connection con,
            MutationTrigger2 trigger,
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

    public DeleteResult execute() {

        Set<Object> ids = this.ids;
        if (ids == null) {
            return new DeleteResult(Collections.emptyMap());
        }
        this.ids = null;

        SaveContext saveCtx = new SaveContext(
                saveOptions(),
                ctx.con,
                ctx.path.getType(),
                ctx.trigger,
                ctx.affectedRowCountMap
        );
        List<MiddleTableOperator> middleOperators = AbstractOperator.createMiddleTableOperators(
                ctx.options.getSqlClient(),
                ctx.path,
                DisconnectingType.PHYSICAL_DELETE,
                prop -> new MiddleTableOperator(saveCtx.prop(prop)),
                backProp -> new MiddleTableOperator(saveCtx.backProp(backProp))
        );
        for (MiddleTableOperator middleTableOperator : middleOperators) {
            middleTableOperator.disconnect(ids);
        }
        List<ChildTableOperator> subOperators = AbstractOperator.createSubOperators(
                ctx.options.getSqlClient(),
                ctx.path,
                DisconnectingType.PHYSICAL_DELETE,
                backProp -> new ChildTableOperator(ctx.backPropOf(backProp))
        );
        for (ChildTableOperator subOperator : subOperators) {
            subOperator.disconnect(ids);
        }

        int rowCount = executeImpl(ids);
        if (ctx.trigger != null) {
            ctx.trigger.submit(ctx.options.getSqlClient(), ctx.con);
        }

        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
        return new DeleteResult(ctx.affectedRowCountMap);
    }

    private int executeImpl(Collection<Object> ids) {
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
            MutationTrigger2 trigger,
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
        LogicalDeletedValueGenerator<?> generator =
                LogicalDeletedValueGenerators.of(info, sqlClient);
        return deleteWithoutTrigger(
                sqlClient,
                con,
                type,
                ids,
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
            MutationTrigger2 trigger,
            LogicalDeletedInfo info
    ) {
        Map<Object, ImmutableSpi> rowMap = (Map<Object, ImmutableSpi>)
                sqlClient
                .caches(CacheDisableConfig::disableAll)
                .getEntities()
                .forConnection(con)
                .findMapByIds(type.getJavaClass(), ids);
        LogicalDeletedValueGenerator<?> generator =
                LogicalDeletedValueGenerators.of(info, sqlClient);
        Object generatedValue = generator != null ? generator.generate() : null;
        Iterator<Object> idItr = ids.iterator();
        while (idItr.hasNext()) {
            Object id = idItr.next();
            ImmutableSpi oldRow = rowMap.get(id);
            if (oldRow == null) {
                idItr.remove();
            } else if (info != null) {
                ImmutableSpi newRow = (ImmutableSpi) Internal.produce(type, oldRow, draft -> {
                    ((DraftSpi)draft).__set(info.getProp().getId(), generatedValue);
                });
                trigger.modifyEntityTable(oldRow, newRow);
            }
        }
        if (ids.isEmpty()) {
            return 0;
        }
        return deleteWithoutTrigger(sqlClient, con, type, ids, info, generatedValue);
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
                ExecutionPurpose.DELETE,
                null,
                PreparedStatement::executeUpdate
        );
        return sqlClient.getExecutor().execute(args);
    }

    private SaveOptions saveOptions() {
        DeleteOptions options = ctx.options;
        return new SaveOptions() {
            @Override
            public JSqlClientImplementor getSqlClient() {
                return options.getSqlClient();
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
            public Set<ImmutableProp> getKeyProps(ImmutableType type) {
                return Collections.emptySet();
            }

            @Override
            public LockMode getLockMode() {
                return LockMode.AUTO;
            }

            @Override
            public UserOptimisticLock<?, ?> getUserOptimisticLock(ImmutableType type) {
                return null;
            }

            @Override
            public boolean isAutoCheckingProp(ImmutableProp prop) {
                return false;
            }
        };
    }
}
