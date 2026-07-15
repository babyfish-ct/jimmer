package org.babyfish.jimmer.json;

import org.jetbrains.annotations.NotNull;

public class LongToStringConverter implements Converter<Long, String> {

    @NotNull
    @Override
    public String output(@NotNull Long value) {
        return Long.toString(value);
    }

    @NotNull
    @Override
    public Long input(@NotNull String jsonValue) {
        return Long.parseLong(jsonValue);
    }
}
