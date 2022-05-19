package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;

import java.util.Map;
import java.util.stream.IntStream;

class DefaultRecursionStrategy<E> implements RecursionStrategy<E> {

    private int depth;

    private static final DefaultRecursionStrategy<?>[] DEFAULTS =
            IntStream.range(0, 10)
                    .mapToObj(it -> new DefaultRecursionStrategy<Object>(it + 1))
                    .toArray(DefaultRecursionStrategy[]::new);

    private static final DefaultRecursionStrategy<?> UNLIMITED =
            new DefaultRecursionStrategy<>(Integer.MAX_VALUE);

    @SuppressWarnings("unchecked")
    public static <E> DefaultRecursionStrategy<E> of(int depth) {
        if (depth == Integer.MAX_VALUE) {
            return (DefaultRecursionStrategy<E>) UNLIMITED;
        }
        int defaultIndex = depth - 1;
        if (defaultIndex < DEFAULTS.length) {
            return (DefaultRecursionStrategy<E>) DEFAULTS[Math.max(defaultIndex, 0)];
        }
        return new DefaultRecursionStrategy<E>(depth);
    }

    private DefaultRecursionStrategy(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public boolean isFetchable(E entity, int depth) {
        return depth < this.depth;
    }
}
