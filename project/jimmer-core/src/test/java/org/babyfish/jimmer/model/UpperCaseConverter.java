package org.babyfish.jimmer.model;

import org.babyfish.jimmer.jackson.Converter;
import org.jetbrains.annotations.NotNull;

public class UpperCaseConverter implements Converter<String, String> {

    @NotNull
    @Override
    public String output(@NotNull String value) {
        return value.toUpperCase();
    }

    @NotNull
    @Override
    public String input(@NotNull String value) {
        return value.toLowerCase();
    }
}
