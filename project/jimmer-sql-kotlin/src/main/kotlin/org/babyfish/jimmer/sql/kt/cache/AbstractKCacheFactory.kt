package org.babyfish.jimmer.sql.kt.cache

import org.babyfish.jimmer.sql.cache.FilterState

abstract class AbstractKCacheFactory : KFilterStateAwareCacheFactory {

    private lateinit var _filterState: FilterState

    override fun setFilterState(filterState: FilterState) {
        _filterState = filterState
    }

    protected val filterState: FilterState
        get() = _filterState
}