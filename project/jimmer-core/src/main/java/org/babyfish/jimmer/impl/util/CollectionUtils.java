package org.babyfish.jimmer.impl.util;

import java.util.Collection;
import java.util.List;

public class CollectionUtils {

    private CollectionUtils() {}

    public static <E> E first(Collection<E> c) {
        if (c instanceof List<?>) {
            return ((List<E>)c).get(0);
        }
        return c.iterator().next();
    }
}
