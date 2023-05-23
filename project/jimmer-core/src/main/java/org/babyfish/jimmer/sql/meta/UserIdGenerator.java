package org.babyfish.jimmer.sql.meta;

public interface UserIdGenerator<T> extends IdGenerator {

    T generate(Class<?> entityType);

    final class None implements UserIdGenerator<Object> {

        @Override
        public Object generate(Class<?> entityType) {
            throw new UnsupportedOperationException();
        }
    }
}
