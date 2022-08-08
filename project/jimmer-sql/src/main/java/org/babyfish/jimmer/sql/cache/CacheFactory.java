package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CacheFactory {

    @Nullable
    default Cache<?, ?> createObjectCache(@NotNull ImmutableType type) {
        return null;
    }

    @Nullable
    default Cache<?, ?> createAssociatedIdCache(@NotNull ImmutableProp prop) {
        return null;
    }

    @Nullable
    default Cache<?, List<?>> createAssociatedIdListCache(@NotNull ImmutableProp prop) {
        return null;
    }

    @Nullable
    default Cache<?, ?> createResolverCache(@NotNull ImmutableProp prop) {
        return null;
    }
}
