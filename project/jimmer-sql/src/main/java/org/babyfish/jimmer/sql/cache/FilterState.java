package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableType;

public interface FilterState {

    /**
     * Check whether some global filters have been applied to an entity type
     * @param type An entity type
     * @return Whether some global filters have been applied
     */
    boolean isAffected(ImmutableType type);
}
