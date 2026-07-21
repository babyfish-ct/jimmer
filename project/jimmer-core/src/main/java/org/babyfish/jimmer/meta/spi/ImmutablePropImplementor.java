package org.babyfish.jimmer.meta.spi;

import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public interface ImmutablePropImplementor {

    @NotNull
    ImmutableType getCacheOwnerType();

    int getPropCacheSlot();

    int getAssociationOrdinal();

    Method getJavaGetter();
}
