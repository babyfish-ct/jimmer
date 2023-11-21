package org.babyfish.jimmer.sql.meta;

public interface LogicalDeletedValueGenerator<T> {

    T generate(Class<?> entityType, Object id);

    class None implements LogicalDeletedValueGenerator<Object> {

        @Override
        public Object generate(Class<?> entityType, Object id) {
            throw new UnsupportedOperationException();
        }
    }
}
