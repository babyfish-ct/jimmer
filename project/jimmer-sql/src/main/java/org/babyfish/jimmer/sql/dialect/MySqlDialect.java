package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.ast.impl.query.ForUpdate;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.query.LockWait;

import java.util.function.IntSupplier;

/**
 * MySQL 8.x
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
                .enter(AbstractSqlBuilder.ScopeType.SET);
        if (ctx.isIdInteger()) {
            ctx
                    .separator()
                    .appendId()
                    .sql(" = last_insert_id(")
                    .appendId()
                    .sql(")");
        }
        ctx
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
                    .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                    .appendInsertedColumns("")
                    .leave()
                    .sql(" values")
                    .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                    .appendInsertingValues()
                    .leave();
        } else {
            ctx.sql("insert into ")
                    .appendTableName()
                    .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                    .appendInsertedColumns("")
                    .leave()
                    .enter(AbstractSqlBuilder.ScopeType.VALUES)
                    .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                    .appendInsertingValues()
                    .leave()
                    .leave()
                    .sql(" on duplicate key update ")
                    .enter(AbstractSqlBuilder.ScopeType.COMMA);
            if (ctx.hasGeneratedId() && ctx.isIdInteger()) {
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

    @Override
    public void renderForUpdate(AbstractSqlBuilder<?> builder, ForUpdate forUpdate) {
        builder.sql(" for ");
        switch (forUpdate.getLockMode()) {
            case UPDATE:
                builder.sql("update");
                break;
            case SHARE:
                builder.sql("share");
                break;
            default:
                throw new IllegalArgumentException(
                        "MySQL8 does not support the lock mode \"" +
                                forUpdate.getLockMode() +
                                "\""
                );
        }
        LockWait wait = forUpdate.getLockWait();
        if (wait == LockWait.NO_WAIT) {
            builder.sql(" no wait");
        } else if (wait == LockWait.SKIP_LOCKED) {
            if (forUpdate.getLockMode().isShared()) {
                throw new IllegalArgumentException(
                        "MySQL8 does not support LockMode.SHARE and LockWait.SKIP_LOCKED"
                );
            }
            builder.sql(" skip locked");
        } else if (wait instanceof IntSupplier) {
            throw new IllegalArgumentException(
                    "MySQL8 does not support " + wait
            );
        }
    }
}
