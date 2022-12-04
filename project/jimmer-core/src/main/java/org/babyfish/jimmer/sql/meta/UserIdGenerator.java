package org.babyfish.jimmer.sql.meta;

public interface UserIdGenerator<T> extends IdGenerator {

    T generate(Class<?> entityType);
}
