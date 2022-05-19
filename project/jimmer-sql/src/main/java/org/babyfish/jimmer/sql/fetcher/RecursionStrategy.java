package org.babyfish.jimmer.sql.fetcher;

public interface RecursionStrategy<E> {

    boolean isFetchable(E entity, int depth);
}
