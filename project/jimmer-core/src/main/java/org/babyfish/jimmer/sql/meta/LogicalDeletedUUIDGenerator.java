package org.babyfish.jimmer.sql.meta;

import java.util.UUID;

public class LogicalDeletedUUIDGenerator implements LogicalDeletedValueGenerator<UUID> {

    @Override
    public UUID generate(Class<?> entityType, Object id) {
        return UUID.randomUUID();
    }
}
