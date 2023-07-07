package org.babyfish.jimmer.util;

import org.babyfish.jimmer.impl.util.StringUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringUtilTest {

    @Test
    public void test() {

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
