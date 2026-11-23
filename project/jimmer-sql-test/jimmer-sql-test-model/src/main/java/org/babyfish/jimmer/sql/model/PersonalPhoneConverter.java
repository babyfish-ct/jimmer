package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.json.converter.Converter;
import org.jspecify.annotations.NonNull;

public class PersonalPhoneConverter implements Converter<String, String> {
    @Override
    public @NonNull String output(@NonNull String value) {
        return value.substring(0, 3) + "****" + value.substring(7);
    }
}
