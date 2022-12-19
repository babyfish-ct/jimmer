package org.babyfish.jimmer.spring.model;

import org.jetbrains.annotations.NotNull;

public interface Input<E> {

    @NotNull
    E toEntity();
}
