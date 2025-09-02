package org.babyfish.jimmer.sql.runtime;

public interface TupleCreator<T> {

    T createTuple(Object[] args);
}
