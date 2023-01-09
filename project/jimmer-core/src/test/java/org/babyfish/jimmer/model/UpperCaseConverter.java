package org.babyfish.jimmer.model;

import com.fasterxml.jackson.databind.util.StdConverter;

public class UpperCaseConverter extends StdConverter<String, String> {

    @Override
    public String convert(String value) {
        return value.toUpperCase();
    }
}
