package org.babyfish.jimmer.lang;

import java.util.function.Supplier;

public class Lazy<T> {

    private final Supplier<T> supplier;

    private T value;

    private boolean initialized;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (initialized) {
            return value;
        }
        value = supplier.get();
        initialized = true;
        return value;
    }
}
