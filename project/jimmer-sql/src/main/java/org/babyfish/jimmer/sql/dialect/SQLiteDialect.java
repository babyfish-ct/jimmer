package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;

import java.util.List;

public class SQLiteDialect extends DefaultDialect {
    @Override
    public boolean isDeleteAliasSupported() {
        return false;
    }

    @Override
    public boolean isUpdateAliasSupported() {
        return false;
    }

    @Override
    public UpdateJoin getUpdateJoin() {
        return new UpdateJoin(false, UpdateJoin.From.AS_JOIN);
    }

    @Override
    public boolean isUpsertSupported() {
        return true;
    }

    @Override
    public void upsert(UpsertContext ctx) {
        ctx.sql("insert into ")
                .appendTableName()
                .enter(AbstractSqlBuilder.ScopeType.LIST)
                .appendInsertedColumns("")
                .leave()
                .enter(AbstractSqlBuilder.ScopeType.VALUES)
                .enter(AbstractSqlBuilder.ScopeType.LIST)
                .appendInsertingValues()
                .leave()
                .leave()
                .sql(" on conflict")
                .enter(AbstractSqlBuilder.ScopeType.LIST)
                .appendConflictColumns()
                .leave();
        if (ctx.isUpdateIgnored()) {
            ctx.sql(" do nothing");
        } else if (ctx.hasUpdatedColumns()) {
            ctx.sql(" do update")
                    .enter(AbstractSqlBuilder.ScopeType.SET)
                    .appendUpdatingAssignments("excluded.", "")
                    .leave();
            if (ctx.hasOptimisticLock()) {
                ctx.sql(" where ").appendOptimisticLockCondition("excluded.");
            }
        } else if (ctx.hasGeneratedId()) {
            ctx.sql(" do update set ");
            List<ValueGetter> conflictGetters = ctx.getConflictGetters();
            ValueGetter cheapestGetter = conflictGetters.get(0);
            for (ValueGetter getter : conflictGetters) {
                Class<?> type = getter.metadata().getValueProp().getReturnClass();
                type = Classes.boxTypeOf(type);
                if (type == Boolean.class || Number.class.isAssignableFrom(type)) {
                    cheapestGetter = getter;
                    break;
                }
            }
            ctx.sql(cheapestGetter).sql(" = excluded.").sql(cheapestGetter);
        } else {
            ctx.sql(" do nothing");
        }
    }
}
