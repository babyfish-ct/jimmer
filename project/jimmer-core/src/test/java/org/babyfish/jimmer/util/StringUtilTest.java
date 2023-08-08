package org.babyfish.jimmer.util;

import org.babyfish.jimmer.impl.util.StringUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringUtilTest {

    @Test
    public void testIdentifier() {
        Assertions.assertEquals(
                "unique",
                StringUtil.identifier("Unique")
        );

        Assertions.assertEquals(
                "uniqueClientId",
                StringUtil.identifier("Unique", "client", "id")
        );

        Assertions.assertEquals(
                "uniqueClientId",
                StringUtil.identifier("UniqueC", "lientI", "d")
        );
    }

    @Test
    public void testSnake() {

        Assertions.assertEquals(
                "Super_Cool",
                StringUtil.snake("SuperCool", StringUtil.SnakeCase.ORIGINAL)
        );
        Assertions.assertEquals(
                "super_cool",
                StringUtil.snake("SuperCool", StringUtil.SnakeCase.LOWER)
        );
        Assertions.assertEquals(
                "SUPER_COOL",
                StringUtil.snake("SuperCool", StringUtil.SnakeCase.UPPER)
        );

        Assertions.assertEquals(
                "super_Cool",
                StringUtil.snake("superCool", StringUtil.SnakeCase.ORIGINAL)
        );
        Assertions.assertEquals(
                "super_cool",
                StringUtil.snake("superCool", StringUtil.SnakeCase.LOWER)
        );
        Assertions.assertEquals(
                "SUPER_COOL",
                StringUtil.snake("superCool", StringUtil.SnakeCase.UPPER)
        );
    }
}
