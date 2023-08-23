package org.babyfish.jimmer;

/**
 * Static type can be created by dynamic immutable object.
 */
public interface View<E> {
    E toEntity();
}
