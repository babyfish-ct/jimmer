package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

public interface FilterState {

    /**
     * Check whether some global filters have been applied to an entity type
     *
     * <p>You can use this method to check whether an association property should be multi-view cache,
     * by check its {@link ImmutableProp#getTargetType() target} type</p>
     *
     * Unfortunately, there is no way to tell you that a calculated property should be multi-view cache,
     * using your business knowledge to judge is the only way.
     *
     * @param type An entity type
     * @return Whether some global filters have been applied
     */
    boolean isAffected(ImmutableType type);
}
