package org.babyfish.jimmer;

/**
 * Super interface for static input-only DTO
 *
 * @param <E> The dynamic entity interface type
 */
public interface Input<E> {

    E toEntity();
}
