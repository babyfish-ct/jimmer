package org.babyfish.jimmer.sql.meta;

import java.util.UUID;

public class UUIDIdGenerator implements UserIdGenerator {

    @Override
    public Object generate(Class<?> entityType) {
        return UUID.randomUUID();
    }
}
