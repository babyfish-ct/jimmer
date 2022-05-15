package org.babyfish.jimmer.sql.meta;

public interface UserIdGenerator extends IdGenerator {

    Object generate(Class<?> entityType);
}
