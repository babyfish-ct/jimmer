package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.jackson.Converter;
import org.jetbrains.annotations.NotNull;

public class PersonalPhoneConverter implements Converter<String, String> {
    @Override
    public @NotNull String output(@NotNull String value) {
        return value.substring(0, 3) + "****" + value.substring(7);
    }
}
