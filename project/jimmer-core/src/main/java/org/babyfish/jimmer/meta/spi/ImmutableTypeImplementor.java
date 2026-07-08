package org.babyfish.jimmer.meta.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.jetbrains.annotations.Nullable;

public interface ImmutableTypeImplementor {

    @Nullable
    ImmutableProp getFakeUpdateProp();
}
