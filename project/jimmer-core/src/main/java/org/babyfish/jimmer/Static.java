package org.babyfish.jimmer;

import org.jetbrains.annotations.NotNull;

public interface Static<E> {

    @NotNull
    E toEntity();

    @NotNull
    E toEntity(E base);
}
