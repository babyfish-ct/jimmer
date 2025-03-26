package org.babyfish.jimmer.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;

public class JsonAssertions {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    public static void assertJsonEquals(String expected, String actual) {
        try {
            Assertions.assertEquals(MAPPER.readTree(expected), MAPPER.readTree(actual));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Input strings are not json", e);
        }
    }
}
