package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableType;

public interface FilterState {

    /**
     * Check whether some global filters has been applied to an entity type
     * @param type
     * @return
     */
    boolean isAffected(ImmutableType type);
}
