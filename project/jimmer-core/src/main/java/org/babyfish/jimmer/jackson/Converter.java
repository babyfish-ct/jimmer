package org.babyfish.jimmer.jackson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @see LongToStringConverter
 * @see LongListToStringListConverter
 */
public interface Converter<S, T> {

    @NotNull
    T output(@NotNull S value);

    @NotNull
    default S input(@NotNull T jsonValue) {
        throw new UnsupportedOperationException(
                "\"" +
                        this.getClass().getName() +
                        "\" does not support the `input` method"
        );
    }
}
