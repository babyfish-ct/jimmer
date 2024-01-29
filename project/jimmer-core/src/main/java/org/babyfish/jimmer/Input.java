package org.babyfish.jimmer;

import org.babyfish.jimmer.client.ApiIgnore;
import org.jetbrains.annotations.NotNull;

/**
 * Super interface for static input-only DTO
 *
 * @param <E> The dynamic entity interface type
 */
@ApiIgnore
public interface Input<E> extends View<E> {

    E toEntity();

    /**
     * For complex form contains UI tab.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    static <E> E toEntity(Input<E> ... inputs) {
        E[] entities = (E[])new Object[inputs.length];
        for (int i = inputs.length - 1; i >= 0; --i) {
            entities[i] = inputs[i] != null ? inputs[i].toEntity() : null;
        }
        return ImmutableObjects.merge(entities);
    }
}
