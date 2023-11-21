package org.babyfish.jimmer.sql.meta;

public interface LogicalDeletedValueGenerator<T> {

    T generate(Class<?> entityType);

    class None implements LogicalDeletedValueGenerator<Object> {

        @Override
        public Object generate(Class<?> entityType) {
            throw new UnsupportedOperationException();
        }
    }
}
