package org.babyfish.jimmer;

import org.babyfish.jimmer.client.ApiIgnore;

/**
 * Interface for generated DTO class of entity type
 */
@ApiIgnore
public interface View<E> extends Dto<E> {

    E toEntity();

    @Override
    default E toImmutable() {
        return toEntity();
    }
}
