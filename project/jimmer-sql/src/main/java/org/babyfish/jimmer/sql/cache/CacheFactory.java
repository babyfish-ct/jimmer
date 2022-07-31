package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CacheFactory {

    @NotNull
    default Cache<?, ?> createObjectCache(@NotNull ImmutableType type) {
        return null;
    }

    @NotNull
    default Cache<?, ?> createAssociatedIdCache(@NotNull ImmutableProp type) {
        return null;
    }

    @NotNull
    default Cache<?, List<?>> createAssociatedIdListCache(@NotNull ImmutableProp type) {
        return null;
    }
}
