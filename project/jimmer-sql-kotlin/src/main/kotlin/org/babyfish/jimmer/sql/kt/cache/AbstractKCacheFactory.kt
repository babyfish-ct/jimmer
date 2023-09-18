package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.sql.cache.FilterState

abstract class AbstractKCacheFactory : KFilterStateAwareCacheFactory {

    private lateinit var _filterState: FilterState

    /**
     * This method is invoked by Jimmer, it can be ignored by user.
     * @param filterState
     */
    override fun setFilterState(filterState: FilterState) {
        _filterState = filterState
    }

    /**
     * Get the filter state object which can help you to simply your code.
     * @return The filter state which can tell you whether an entity type is affected by global filters.
     */
    protected val filterState: FilterState
        get() = _filterState
}