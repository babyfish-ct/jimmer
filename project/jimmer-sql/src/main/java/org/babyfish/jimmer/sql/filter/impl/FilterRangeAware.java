package org.babyfish.jimmer.sql.filter.impl;

import org.babyfish.jimmer.meta.ImmutableType;

import java.util.Set;

/**
 * This interface should be implemented by user-defined `CacheFactory`
 */
public interface FilterRangeAware {

    void setAffectedTypes(Set<ImmutableType> types);
}
