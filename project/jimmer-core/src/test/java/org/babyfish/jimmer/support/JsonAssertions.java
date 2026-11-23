package org.babyfish.jimmer.support;

import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.json.JsonReader;
import org.junit.jupiter.api.Assertions;

public class JsonAssertions {

    public static void assertJsonEquals(JsonCodec codec, String expected, String actual) {
        JsonReader<?> reader = codec.treeReader();
        try {
            Assertions.assertEquals(reader.read(expected), reader.read(actual));
        } catch (Exception e) {
            throw new IllegalArgumentException("Input strings are not json", e);
        }
    }
}
