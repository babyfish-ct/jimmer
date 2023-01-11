package org.babyfish.jimmer;

import org.jetbrains.annotations.NotNull;

public interface Input<E> {

    @NotNull
    E toEntity();
}
