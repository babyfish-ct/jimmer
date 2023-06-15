package org.babyfish.jimmer.sql.cache;

public abstract class AbstractCacheFactory implements FilterStateAwareCacheFactory {

    private FilterState filterState;

    @Override
    public void setFilterState(FilterState filterState) {
        this.filterState = filterState;
    }

    protected final FilterState getFilterState() {
        return filterState;
    }
}
