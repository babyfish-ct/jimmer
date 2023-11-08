package org.babyfish.jimmer.apt.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

public class DtoContextTest {

    @Test
    public void testStandardDtoDirs() {

        Assertions.assertEquals(
                "[src/main/dto]",
                DtoContext.standardDtoDirs(Collections.singleton("src/main/dto")).toString()
        );

        Assertions.assertEquals(
                "[src/main/dto/module1, src/main/dto/module2]",
                DtoContext.standardDtoDirs(
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
                DtoContext.standardDtoDirs(
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
                DtoContext.standardDtoDirs(
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
                DtoContext.standardDtoDirs(
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
