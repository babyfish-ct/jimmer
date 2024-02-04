package org.babyfish.jimmer.sql.meta;

public interface LogicalDeletedValueGenerator<T> {

    T generate();

    class None implements LogicalDeletedValueGenerator<Object> {

        @Override
        public Object generate() {
            throw new UnsupportedOperationException();
        }
    }
}
