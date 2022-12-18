package org.babyfish.jimmer.spring.repository.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Iterables {

    private Iterables() {}

    public static <E> Collection<E> toCollection(Iterable<E> iterable) {
        if (iterable instanceof Collection<?>) {
            return (Collection<E>) iterable;
        }
        if (iterable == null) {
            return Collections.emptyList();
        }
        List<E> list = new ArrayList<>();
        for (E e : iterable) {
            list.add(e);
        }
        return list;
    }
}
