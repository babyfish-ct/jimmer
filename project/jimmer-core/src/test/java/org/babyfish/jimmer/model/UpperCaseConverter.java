package org.babyfish.jimmer.model;

import org.babyfish.jimmer.jackson.Converter;

public class UpperCaseConverter implements Converter<String> {

    @Override
    public String output(String value) {
        return value.toUpperCase();
    }

    @Override
    public String input(String value) {
        return value.toLowerCase();
    }
}
