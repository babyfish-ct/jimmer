package org.babyfish.jimmer.support;

import org.babyfish.jimmer.json.codec.JsonReader;
import org.junit.jupiter.api.Assertions;

import static org.babyfish.jimmer.json.codec.JsonCodec.defaultCodec;

public class JsonAssertions {
    private static final JsonReader<?> READER = defaultCodec().treeReader();

    public static void assertJsonEquals(String expected, String actual) {
        try {
            Assertions.assertEquals(READER.read(expected), READER.read(actual));
        } catch (Exception e) {
            throw new IllegalArgumentException("Input strings are not json", e);
        }
    }
}
