package org.babyfish.jimmer.impl.util;

import java.util.function.Function;

public class ClassCache<V> extends ClassValue<V> {

    private final Function<Class<?>, V> creator;

    public ClassCache(Function<Class<?>, V> creator) {
        this.creator = creator;
    }

    @Override
    protected V computeValue(Class<?> type) {
        return creator.apply(type);
    }
}
