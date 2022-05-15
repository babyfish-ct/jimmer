package org.babyfish.jimmer.sql.meta;

import java.util.UUID;

public class UUIDIdGenerator implements UserIdGenerator {

    public static final String FULL_NAME = "org.babyfish.jimmer.sql.meta.UUIDIdGenerator";

    @Override
    public Object generate(Class<?> entityType) {
        return UUID.randomUUID();
    }
}
