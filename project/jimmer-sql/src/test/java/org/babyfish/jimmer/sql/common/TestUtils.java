package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.support.JsonAssertions;
import org.junit.jupiter.api.Assertions;

public class TestUtils {

    public static void expect(String json, Object o) {
        Assertions.assertEquals(
                json == null,
                o == null,
                "The nullity of json and object must be same"
        );
        if (o != null && json != null) {
            String normalizedJson = json.replace("--->", "");
            String actualString = o.toString();

            // Try to parse as JSON and compare semantically to handle property ordering issues
            try {
                JsonAssertions.assertJsonEquals(normalizedJson, actualString);
            } catch (Exception e) {
                // Fall back to string comparison if JSON parsing fails
                Assertions.assertEquals(normalizedJson, actualString);
            }
        }
    }
}
