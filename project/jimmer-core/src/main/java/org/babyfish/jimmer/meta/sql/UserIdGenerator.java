package org.babyfish.jimmer.meta.sql;

public interface UserIdGenerator extends IdGenerator {

    Object generate(Class<?> entityType);
}
