package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.sql.cache.FilterState
import org.babyfish.jimmer.sql.cache.FilterStateAware

interface KFilterStateAwareCacheFactory : KCacheFactory, FilterStateAware {

    /**
     * This method is invoked by Jimmer, it can be ignored by user.
     * @param filterState
     */
    override fun setFilterState(filterState: FilterState)
}