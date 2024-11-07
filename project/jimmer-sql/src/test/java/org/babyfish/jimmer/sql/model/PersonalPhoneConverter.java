package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.jackson.Converter;

public class PersonalPhoneConverter implements Converter<String, String> {
    @Override
    public String output(String value) {
        return value.substring(0, 3) + "****" + value.substring(7);
    }
}
