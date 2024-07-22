package org.babyfish.jimmer.impl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class CollectionUtils {

    private CollectionUtils() {}

    public static <T> List<T> toListOrNull(T[] array) {
        if (array == null) {
            return null;
        }

        return Arrays.asList(array);
    }

    public static <E> E first(Iterable<E> i) {
        if (i instanceof List<?>) {
            return ((List<E>)i).get(0);
        }
        return i.iterator().next();
    }

    public static <E, T> List<T> map(Iterable<E> i, Function<E, T> mapper) {
        List<T> list;
        if (i instanceof Collection<?>) {
            list = new ArrayList<>(((Collection<?>) i).size());
        } else {
            list = new ArrayList<>();
        }
        for (E e : i) {
            if (e != null) {
                list.add(mapper.apply(e));
            }
        }
        return list;
    }
}
