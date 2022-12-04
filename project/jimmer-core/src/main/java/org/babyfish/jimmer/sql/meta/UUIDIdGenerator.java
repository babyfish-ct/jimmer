package org.babyfish.jimmer.sql.meta;

import java.util.UUID;

public class UUIDIdGenerator implements UserIdGenerator<UUID> {

    @Override
    public UUID generate(Class<?> entityType) {
        return UUID.randomUUID();
    }
}
