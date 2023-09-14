package org.babyfish.jimmer.sql.filter.impl;

import org.babyfish.jimmer.meta.ImmutableType;

/**
 * Only used by jimmer internally
 */
public interface FilterWrapper {

    ImmutableType getImmutableType();

    Class<?> getFilterType();

    default Object unwrap() {
        return null;
    }

    static Object unwrap(Object filter) {
        while (filter instanceof FilterWrapper) {
            Object unwrappedFilter = ((FilterWrapper)filter).unwrap();
            if (unwrappedFilter == null) {
                return filter;
            }
            filter = unwrappedFilter;
        }
        return filter;
    }
}
