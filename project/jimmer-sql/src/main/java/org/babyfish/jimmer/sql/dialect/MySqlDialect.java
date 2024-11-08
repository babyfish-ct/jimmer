package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;



/**
 * For MySQL or TiDB
 */
public class MySqlDialect extends MySql5Dialect {

    @Override
    public boolean isUpsertSupported() {
        return true;
    }

    @Override
    public void update(UpdateContext ctx) {
        if (!ctx.isUpdatedByKey()) {
            super.update(ctx);
            return;
        }
        ctx
                .sql("update ")
                .appendTableName()
                .enter(AbstractSqlBuilder.ScopeType.SET)
                .separator()
                .appendId()
                .sql(" = last_insert_id(")
                .appendId()
                .sql(")")
                .appendAssignments()
                .leave()
                .enter(AbstractSqlBuilder.ScopeType.WHERE)
                .appendPredicates()
                .leave();
    }

    @Override
    public void upsert(UpsertContext ctx) {
        if (ctx.isUpdateIgnored() || (!ctx.hasUpdatedColumns() && !ctx.hasGeneratedId())) {
            ctx.sql("insert ignore into ")
                    .appendTableName()
                    .enter(AbstractSqlBuilder.ScopeType.LIST)
                    .appendInsertedColumns("")
                    .leave()
                    .enter(AbstractSqlBuilder.ScopeType.VALUES)
                    .enter(AbstractSqlBuilder.ScopeType.LIST)
                    .appendInsertingValues()
                    .leave()
                    .leave();
        } else {
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
                    .sql(" on duplicate key update ")
                    .enter(AbstractSqlBuilder.ScopeType.COMMA);
            if (ctx.hasGeneratedId()) {
                ctx.separator()
                        .sql(FAKE_UPDATE_COMMENT)
                        .sql(" ")
                        .appendGeneratedId()
                        .sql(" = ")
                        .sql("last_insert_id(")
                        .appendGeneratedId()
                        .sql(")");
            }
            if (ctx.hasUpdatedColumns()) {
                ctx.separator().appendUpdatingAssignments("values(", ")");
            }
            ctx.leave();
        }
    }
}
