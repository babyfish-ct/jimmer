package org.babyfish.jimmer;

import org.babyfish.jimmer.client.ApiIgnore;

/**
 * Super interface for static input-only DTO
 *
 * @param <E> The dynamic entity interface type
 */
@ApiIgnore
public interface Input<E> {

    E toEntity();
}
