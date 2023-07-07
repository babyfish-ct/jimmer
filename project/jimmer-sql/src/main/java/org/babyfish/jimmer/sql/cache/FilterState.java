package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableType;

public interface FilterState {

    boolean isAffected(ImmutableType type);
}
