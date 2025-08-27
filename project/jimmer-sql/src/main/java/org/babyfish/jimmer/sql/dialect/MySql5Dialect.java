package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.ast.impl.query.ForUpdate;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.query.LockWait;

/**
 * MySQL 5.x
 */
public class MySql5Dialect extends MySqlStyleDialect {

    @Override
    public boolean isExplicitBatchRequired() {
        return true;
    }

    @Override
    public boolean isBatchDumb() {
        return true;
    }

    @Override
    public void renderForUpdate(AbstractSqlBuilder<?> builder, ForUpdate forUpdate) {
        if (forUpdate.getLockMode().isShared() || forUpdate.getLockWait() != LockWait.DEFAULT) {
            throw new IllegalArgumentException("MySql5 only support LockMode.UPDATE and LockWait.DEFAULT");
        }
        builder.sql(" for update");
    }
}
