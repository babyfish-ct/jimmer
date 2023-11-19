package org.babyfish.jimmer.jackson;

/**
 * @see LongConverter
 * @see LongListConverter
 */
public interface Converter<S, T> {

    T output(S value);

    default S input(T jsonValue) {
        throw new UnsupportedOperationException(
                "\"" +
                        this.getClass().getName() +
                        "\" does not support the `input` method"
        );
    }
}
