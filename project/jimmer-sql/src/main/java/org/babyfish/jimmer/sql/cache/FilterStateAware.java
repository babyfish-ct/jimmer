package org.babyfish.jimmer.sql.cache;

/**
 * This interface should be implemented by user-defined `CacheFactory`
 */
public interface FilterStateAware {

    /**
     * This method is invoked by Jimmer, it can be ignored by user.
     * @param filterState
     */
    void setFilterState(FilterState filterState);
}
