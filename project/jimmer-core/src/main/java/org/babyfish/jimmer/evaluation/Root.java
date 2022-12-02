package org.babyfish.jimmer.evaluation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.util.List;

class Root<T> {

    T result;

    private final Accumulator<T> accumulator;

    public Root(T result, Accumulator<T> accumulator) {
        this.result = result;
        this.accumulator = accumulator;
    }

    public void eval(ImmutableSpi spi) {
        evaluate(null, spi);
    }

    private void evaluate(Path ctx, ImmutableSpi spi) {
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            int propId = prop.getId();
            boolean isLoaded = spi.__isLoaded(propId);
            Object value = spi.__isLoaded(propId) ? spi.__get(propId) : null;
            Path propCtx = new PathImpl(
                    ctx != null ? ctx : this,
                    spi,
                    prop,
                    -1,
                    isLoaded,
                    value
            );
            result = accumulator.accumulate(result, propCtx);
            if (prop.isAssociation(TargetLevel.OBJECT)) {
                if (value instanceof List<?>) {
                    int childIndex = 0;
                    for (Object child : (List<?>) value) {
                        evaluate(
                                new PathImpl(
                                        propCtx,
                                        spi,
                                        prop,
                                        childIndex++,
                                        isLoaded,
                                        child
                                ), (ImmutableSpi) child
                        );
                    }
                } else if (value != null) {
                    evaluate(
                            new PathImpl(
                                    propCtx,
                                    spi,
                                    prop,
                                    0,
                                    isLoaded,
                                    value
                            ),
                            (ImmutableSpi) value
                    );
                }
            }
        }
    }
}