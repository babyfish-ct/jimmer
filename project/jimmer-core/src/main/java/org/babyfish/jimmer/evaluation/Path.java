package org.babyfish.jimmer.evaluation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Path {

    @Nullable
    Path getParent();

    @NotNull ImmutableSpi getSource();

    @NotNull ImmutableProp getProp();

    int getIndex();

    boolean isLoaded();

    Object getValue();
}
