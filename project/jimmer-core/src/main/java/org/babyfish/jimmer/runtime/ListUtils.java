package org.babyfish.jimmer.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

final class ListUtils {

    private static final Class<?> RA_TYPE = Collections.unmodifiableList(new ArrayList<>()).getClass();

    private static final Class<?> NON_RA_TYPE = Collections.unmodifiableList(new LinkedList<>()).getClass();

    private ListUtils() {}

    static <E> List<E> unmodifiable(List<E> list) {
        if (list == null || list.getClass() == RA_TYPE || list.getClass() == NON_RA_TYPE) {
            return list;
        }
        // Java 8 does not reuse existing unmodifiable wrappers.
        return Collections.unmodifiableList(list);
    }
}
