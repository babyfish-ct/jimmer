package org.babyfish.jimmer.sql.model.hr;

import org.babyfish.jimmer.jackson.Converter;
import org.jspecify.annotations.NonNull;

public class ConverterForIssue937 implements Converter<String, String> {

    @NonNull
    @Override
    public String output(@NonNull String value) {
        return value;
    }

    @NonNull
    @Override
    public String input(@NonNull String jsonValue) {
        return jsonValue;
    }
}
