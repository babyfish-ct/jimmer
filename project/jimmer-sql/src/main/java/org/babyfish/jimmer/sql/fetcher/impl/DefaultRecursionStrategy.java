package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.IntStream;

class DefaultRecursionStrategy<E> implements RecursionStrategy<E> {

    private int depth;

    private static final DefaultRecursionStrategy<?>[] DEFAULTS =
            IntStream.range(0, 10)
                    .mapToObj(DefaultRecursionStrategy::new)
                    .toArray(DefaultRecursionStrategy[]::new);

    private static final DefaultRecursionStrategy<?> UNLIMITED =
            new DefaultRecursionStrategy<>(Integer.MAX_VALUE);

    @SuppressWarnings("unchecked")
    public static <E> DefaultRecursionStrategy<E> of(int depth) {
        if (depth == Integer.MAX_VALUE) {
            return (DefaultRecursionStrategy<E>) UNLIMITED;
        }
        if (depth < DEFAULTS.length) {
            return (DefaultRecursionStrategy<E>) DEFAULTS[Math.max(depth, 0)];
        }
        return new DefaultRecursionStrategy<>(depth);
    }

    private DefaultRecursionStrategy(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public boolean isRecursive(@NotNull Args<E> args) {
        return args.getDepth() < this.depth;
    }
}
