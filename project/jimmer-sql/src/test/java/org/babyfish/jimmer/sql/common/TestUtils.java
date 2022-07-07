package org.babyfish.jimmer.sql.common;

import org.junit.jupiter.api.Assertions;

public class TestUtils {

    public static void expect(String json, Object o) {
        Assertions.assertEquals(
                json == null,
                o == null,
                "The nullity of json and object must be same"
        );
        if (o != null && json != null) {
            Assertions.assertEquals(
                    json.replace("--->", ""),
                    o.toString()
            );
        }
    }
}
