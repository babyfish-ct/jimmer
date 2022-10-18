package org.babyfish.jimmer.evaluation;

import org.babyfish.jimmer.runtime.ImmutableSpi;

public class Evaluators {

    private Evaluators() {}

    public static <T> T evaluate(
            Object immutable,
            T initializedValue,
            Accumulator<T> accumulator
    ) {
        if (!(immutable instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("The argument \"immutable\" is not immutable object");
        }
        Root<T> root = new Root<>(initializedValue, accumulator);
        root.eval((ImmutableSpi) immutable);
        return root.result;
    }
}
