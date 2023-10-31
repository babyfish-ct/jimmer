package org.babyfish.jimmer;

/**
 * Static type used to query.
 */
public interface Specification<E> {

    Class<E> getEntityType();
}
