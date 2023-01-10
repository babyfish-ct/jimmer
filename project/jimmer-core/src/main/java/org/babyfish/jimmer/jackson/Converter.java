package org.babyfish.jimmer.jackson;

public interface Converter<T> {

    T output(T value);

    default T input(T value) {
        return value;
    }
}
