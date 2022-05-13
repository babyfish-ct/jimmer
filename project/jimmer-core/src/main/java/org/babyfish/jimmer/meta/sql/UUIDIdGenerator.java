package org.babyfish.jimmer.meta.sql;

import java.util.UUID;

public class UUIDIdGenerator implements UserIdGenerator {

    public static final String FULL_NAME = "org.babyfish.jimmer.meta.sql.UUIDIdGenerator";

    @Override
    public Object generate(Class<?> entityType) {
        return UUID.randomUUID();
    }
}
