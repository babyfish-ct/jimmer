package org.babyfish.jimmer.evaluation;

@FunctionalInterface
public interface Accumulator<T> {

    T accumulate(T base, Path ctx);
}
