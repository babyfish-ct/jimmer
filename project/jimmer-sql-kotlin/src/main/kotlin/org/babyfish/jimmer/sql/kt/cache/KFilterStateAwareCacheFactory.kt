package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.sql.cache.FilterState
import org.babyfish.jimmer.sql.cache.FilterStateAware

interface KFilterStateAwareCacheFactory : KCacheFactory, FilterStateAware {

    override fun setFilterState(filterState: FilterState)
}