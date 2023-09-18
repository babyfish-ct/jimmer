package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.cache.CacheFactory

interface KCacheFactory : CacheFactory {

    /**
     * Create cache for an entity type.
     *
     * <p>If you don't want all entity types to support object caching,
     * you can check the parameters and return null in some cases</p>
     *
     * @see org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
     *
     * @param type The entity type
     * @return Cache object or null
     */
    override fun createObjectCache(type: ImmutableType): Cache<*, *>? =
        null

    /**
     * Create cache for one-to-one/many-to-one association property.
     *
     * <p>If you don't want all one-to-one/many-to-one associations
     * to support object caching, you can check the parameters and
     * return null in some cases</p>
     *
     * @see org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
     *
     * @param prop The one-to-one/many-to-one association property
     * @return Cache object or null
     */
    override fun createAssociatedIdCache(prop: ImmutableProp): Cache<*, *>? =
        null

    /**
     * Create cache for one-to-many/many-to-many association.
     *
     * <p>If you don't want all one-to-many/many-to-many associations
     * to support object caching, you can check the parameters and
     * return null in some cases</p>
     *
     * @see org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
     *
     * @param prop The one-to-many/many-to-many association property
     * @return Cache object or null
     */
    override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>>? =
        null

    /**
     * Create cache for complex calculated property.
     *
     * <p>If you don't want all complex calculated properties
     * to support object caching, you can check the parameters and
     * return null in some cases</p>
     *
     * @see org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder
     *
     * @param prop The complex calculated property
     * @return Cache object or null
     */
    override fun createResolverCache(prop: ImmutableProp): Cache<*, *>? =
        null
}