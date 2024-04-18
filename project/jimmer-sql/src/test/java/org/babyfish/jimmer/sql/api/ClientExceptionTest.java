package org.babyfish.jimmer.sql.api;

import org.babyfish.jimmer.error.CodeBasedRuntimeException;
import org.babyfish.jimmer.ClientException;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
        Assertions.assertEquals("SYSTEM_FAMILY", ex.getFamily());
        Assertions.assertEquals("A", ex.getCode());
        Assertions.assertEquals(
                "{tags=[tag1, tag2], timestamp=2023-12-21T00:00:05, minBound=10, maxBound=20}",
                ex.getFields().toString()
        );
    }

    @Test
    public void testSave() {
        Set<Class<?>> classes = new HashSet<>(
                Arrays.asList(
                        SaveException.class.getAnnotation(ClientException.class)
                                .subTypes()
                )
        );
        for (Class<?> nestedType : SaveException.class.getClasses()) {
            if (CodeBasedRuntimeException.class.isAssignableFrom(nestedType)) {
                Assertions.assertTrue(nestedType.isAnnotationPresent(ClientException.class), nestedType.getName());
                Assertions.assertTrue(classes.contains(nestedType), nestedType.getName());
            }
        }
    }
}
