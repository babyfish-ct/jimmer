package org.babyfish.jimmer;

import org.babyfish.jimmer.client.ApiIgnore;

/**
 * Static type can be created by dynamic immutable object.
 */
@ApiIgnore
public interface View<E> {

    E toEntity();
}
