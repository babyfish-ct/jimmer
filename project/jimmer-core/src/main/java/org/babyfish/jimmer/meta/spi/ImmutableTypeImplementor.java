package org.babyfish.jimmer.meta.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ImmutableTypeImplementor {

    @NotNull
    ImmutableType getCacheOwnerType();

    int getTypeCacheSlot();

    int getTypeCacheSlotCount();

    int getPropCacheSlotCount();

    @Nullable
    ImmutableProp getFakeUpdateProp();
}
