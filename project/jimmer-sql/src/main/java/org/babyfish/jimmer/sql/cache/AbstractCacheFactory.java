package org.babyfish.jimmer.sql.cache;

public abstract class AbstractCacheFactory implements FilterStateAwareCacheFactory {

    private FilterState filterState;

    /**
     * This method is invoked by Jimmer, it can be ignored by user.
     * @param filterState
     */
    @Override
    public void setFilterState(FilterState filterState) {
        this.filterState = filterState;
    }

    /**
     * Get the filter state object which can help you to simply your code.
     * @return The filter state which can tell you whether an entity type is affected by global filters.
     */
    protected final FilterState getFilterState() {
        return filterState;
    }
}
