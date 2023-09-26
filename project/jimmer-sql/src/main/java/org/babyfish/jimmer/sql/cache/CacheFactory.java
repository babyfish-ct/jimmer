package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CacheFactory {

    /**
     * Create cache for an entity type.
     *
     * <p>If you don't want all entity types to support object caching,
     * you can check the parameters and return null in some cases</p>
     *
     * @see org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
     * @param type The entity type
     * @return Cache object or null
     */
    default Cache<?, ?> createObjectCache(ImmutableType type) {
        return null;
    }

    /**
     * Create cache for one-to-one/many-to-one association property.
     *
     * <p>If you don't want all one-to-one/many-to-one associations
     * to support object caching, you can check the parameters and
     * return null in some cases</p>
     *
     * @see org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
     * @param prop The one-to-one/many-to-one association property
     * @return Cache object or null
     */
    default Cache<?, ?> createAssociatedIdCache(ImmutableProp prop) {
        return null;
    }

    /**
     * Create cache for one-to-many/many-to-many association.
     *
     * <p>If you don't want all one-to-many/many-to-many associations
     * to support object caching, you can check the parameters and
     * return null in some cases</p>
     *
     * @see org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
     * @param prop The one-to-many/many-to-many association property
     * @return Cache object or null
     */
    default Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
        return null;
    }

    /**
     * Create cache for complex calculated property.
     *
     * <p>If you don't want all complex calculated properties
     * to support object caching, you can check the parameters and
     * return null in some cases</p>
     *
     * @see org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
     * @param prop The complex calculated property
     * @return Cache object or null
     */
    default Cache<?, ?> createResolverCache(ImmutableProp prop) {
        return null;
    }
}
