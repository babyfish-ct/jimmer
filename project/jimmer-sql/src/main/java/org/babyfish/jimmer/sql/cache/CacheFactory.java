package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

public interface CacheFactory {

    default Cache<?, ?> createObjectCache(ImmutableType type) {
        return null;
    }

    default Cache<?, ?> createAssociatedIdCache(ImmutableProp type) {
        return null;
    }

    default Cache<?, ?> createAssociatedIdListCache(ImmutableProp type) {
        return null;
    }
}
