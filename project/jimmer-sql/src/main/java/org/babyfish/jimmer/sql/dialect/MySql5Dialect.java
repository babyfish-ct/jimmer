package org.babyfish.jimmer.sql.dialect;

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
}
