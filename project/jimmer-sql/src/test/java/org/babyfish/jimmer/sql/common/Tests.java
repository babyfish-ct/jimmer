package org.babyfish.jimmer.sql.common;

import org.junit.jupiter.api.Assertions;

public abstract class Tests {

    public static void assertContentEquals(
            String expected,
            Object actual
    ) {
        Assertions.assertEquals(
                expected.replace("--->", "").replace("\r", "").replace("\n", ""),
                actual.toString()
        );
    }
}
