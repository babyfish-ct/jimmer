package org.babyfish.jimmer.sql.meta;

import java.util.UUID;

public class LogicalDeletedUUIDGenerator implements LogicalDeletedValueGenerator<UUID> {

    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
