package org.babyfish.jimmer.impl.util;

import java.util.function.Function;

public class ClassCache<V> extends StaticCache<Class<?>, V> {

    public ClassCache(Function<Class<?>, V> creator) {
        super(creator, true);
    }

    public ClassCache(Function<Class<?>, V> creator, boolean nullable) {
        super(creator, nullable);
    }
}
