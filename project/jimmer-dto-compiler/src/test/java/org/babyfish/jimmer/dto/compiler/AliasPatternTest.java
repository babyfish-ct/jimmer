package org.babyfish.jimmer.dto.compiler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AliasPatternTest {

    @Test
    public void testJoin() {

        Assertions.assertEquals(
                "unique",
                AliasPattern.join("Unique")
        );

        Assertions.assertEquals(
                "uniqueClientId",
                AliasPattern.join("Unique", "client", "id")
        );

        Assertions.assertEquals(
                "uniqueClientId",
                AliasPattern.join("UniqueC", "lientI", "d")
        );
    }
}
