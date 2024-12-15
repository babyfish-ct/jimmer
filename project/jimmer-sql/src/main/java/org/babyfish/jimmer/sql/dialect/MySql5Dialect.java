package org.babyfish.jimmer.sql.dialect;

/**
 * MySQL 5.x
 */
public class MySql5Dialect extends MySqlStyleDialect {

    @Override
    public boolean isBatchSupportedByDefault() {
        return false;
    }

    @Override
    public boolean isFeedbackBatchSupported() {
        return false;
    }
}
