package org.babyfish.jimmer.sql.model.hr;

import org.babyfish.jimmer.jackson.Converter;
import org.jetbrains.annotations.NotNull;

public class ConverterForIssue937 implements Converter<String, String> {

    @NotNull
    @Override
    public String output(@NotNull String value) {
        return value;
    }

    @NotNull
    @Override
    public String input(@NotNull String jsonValue) {
        return jsonValue;
    }
}
