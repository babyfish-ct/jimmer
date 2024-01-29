package org.babyfish.jimmer.sql.meta;

public class LogicalDeletedLongGenerator implements LogicalDeletedValueGenerator<Long> {

    @Override
    public Long generate() {
        return System.currentTimeMillis();
    }
}
