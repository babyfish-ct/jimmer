package org.babyfish.jimmer.sql.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class ClientExceptionTest {

    @Test
    public void test() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        SystemException ex = new SystemException.A(
                "Hello",
                null,
                Arrays.asList("tag1", "tag2"),
                LocalDateTime.parse("2023-12-21 00:00:05", formatter),
                10,
                20
        );
        Assertions.assertEquals("SYSTEM", ex.getFamily());
        Assertions.assertEquals("A", ex.getCode());
        Assertions.assertEquals(
                "{tags=[tag1, tag2], timestamp=2023-12-21T00:00:05, minBound=10, maxBound=20}",
                ex.getFields().toString()
        );
    }
}
