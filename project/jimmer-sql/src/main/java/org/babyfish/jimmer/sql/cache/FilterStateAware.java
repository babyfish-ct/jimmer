package org.babyfish.jimmer.sql.cache;

/**
 * This interface should be implemented by user-defined `CacheFactory`
 */
public interface FilterStateAware {

    void setFilterState(FilterState filterState);
}
