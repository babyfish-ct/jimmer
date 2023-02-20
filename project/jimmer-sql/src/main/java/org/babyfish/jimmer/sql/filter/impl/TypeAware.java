package org.babyfish.jimmer.sql.filter.impl;

import org.babyfish.jimmer.meta.ImmutableType;

/**
 * Only used by jimmer internally
 */
public interface TypeAware {

    ImmutableType getImmutableType();

    Class<?> getFilterType();

    default Object unwrap() {
        return null;
    }
}
