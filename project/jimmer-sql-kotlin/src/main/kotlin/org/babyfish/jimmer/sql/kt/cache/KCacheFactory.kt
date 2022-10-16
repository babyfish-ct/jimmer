package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.cache.Cache
import org.babyfish.jimmer.sql.cache.CacheFactory

interface KCacheFactory : CacheFactory {

    override fun createAssociatedIdListCache(prop: ImmutableProp): Cache<*, List<*>>?
}