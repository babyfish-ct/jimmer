package org.babyfish.jimmer.dto.compiler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

public class DtoUtilsTest {

    @Test
    public void testStandardDtoDirs() {

        Assertions.assertEquals(
                "[src/main/dto]",
                DtoUtils.standardDtoDirs(Collections.singleton("src/main/dto")).toString()
        );

        Assertions.assertEquals(
                "[src/main/dto/module1, src/main/dto/module2]",
                DtoUtils.standardDtoDirs(
                        Arrays.asList(
                                "/src/main/dto/module1",
                                "src/main/dto/module2/",
                                "/src/main/dto/module1/sub_module1",
                                "src/main/dto/module1/sub_module2/",
                                "/src/main/dto/module2/sub_module1",
                                "src/main/dto/module2/sub_module2/"
                        )
                ).toString()
        );

        Assertions.assertEquals(
                "[src/main/dto]",
                DtoUtils.standardDtoDirs(
                        Arrays.asList(
                                "/src/main/dto/",
                                "/src/main/dto/module1",
                                "src/main/dto/module2/",
                                "/src/main/dto/module1/sub_module1",
                                "src/main/dto/module1/sub_module2/",
                                "/src/main/dto/module2/sub_module1",
                                "src/main/dto/module2/sub_module2/"
                        )
                ).toString()
        );

        Assertions.assertEquals(
                "[src/main/dto/module1, src/main/dto/module2]",
                DtoUtils.standardDtoDirs(
                        Arrays.asList(
                                "/src/main/dto/module1/sub_module1",
                                "src/main/dto/module1/sub_module2/",
                                "/src/main/dto/module2/sub_module1",
                                "src/main/dto/module2/sub_module2/",
                                "/src/main/dto/module1",
                                "src/main/dto/module2/"
                        )
                ).toString()
        );

        Assertions.assertEquals(
                "[src/main/dto]",
                DtoUtils.standardDtoDirs(
                        Arrays.asList(
                                "/src/main/dto/module1/sub_module1",
                                "src/main/dto/module1/sub_module2",
                                "/src/main/dto/module2/sub_module1",
                                "src/main/dto/module2/sub_module2/",
                                "/src/main/dto/module1",
                                "src/main/dto/module2/",
                                "/src/main/dto/"
                        )
                ).toString()
        );
    }
}
