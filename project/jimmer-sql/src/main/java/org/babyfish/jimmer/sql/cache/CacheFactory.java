package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.util.List;

public interface CacheFactory {

    Cache<?, ?> createObjectCache(ImmutableType type);

    Cache<?, ?> createAssociatedIdCache(ImmutableProp prop);

    Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop);
}
