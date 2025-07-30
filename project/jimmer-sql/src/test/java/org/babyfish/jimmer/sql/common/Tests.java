package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.support.JsonAssertions;
import org.junit.jupiter.api.Assertions;

public abstract class Tests {

    public static void assertContentEquals(
            String expected,
            Object actual
    ) {
        String normalizedExpected = expected.replace("--->", "").replace("\r", "").replace("\n", "");
        String actualString = actual.toString();

        // Try to parse as JSON and compare semantically to handle property ordering issues
        try {
            JsonAssertions.assertJsonEquals(normalizedExpected, actualString);
        } catch (Exception e) {
            // Fall back to string comparison if JSON parsing fails
            Assertions.assertEquals(normalizedExpected, actualString);
        }
    }
}
