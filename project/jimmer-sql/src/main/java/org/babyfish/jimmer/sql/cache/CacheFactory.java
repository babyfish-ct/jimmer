package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CacheFactory {

    default Cache<?, ?> createObjectCache(ImmutableType type) {
        return null;
    }

    default Cache<?, ?> createAssociatedIdCache(ImmutableProp type) {
        return null;
    }

    default Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp type) {
        return null;
    }
}
