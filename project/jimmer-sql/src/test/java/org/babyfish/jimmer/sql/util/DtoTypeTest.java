package org.babyfish.jimmer.sql.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.sql.model.inheritance.dto.TheAdministratorInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

public class DtoTypeTest {

    @Test
    public void test() throws JsonProcessingException {
        TheAdministratorInput input = TheAdministratorInput
                .newBuilder()
                .setCreatedTime(LocalDateTime.of(2023, 1, 12, 16, 49, 27))
                .setModifiedTime(LocalDateTime.of(2023, 1, 12, 17, 34, 9))
                .setName("AdminName")
                .setRoles(
                        Arrays.asList(
                                TheAdministratorInput.TargetOf_roles
                                        .newBuilder()
                                        .setCreatedTime(LocalDateTime.of(2023, 1, 12, 16, 49, 27))
                                        .setModifiedTime(LocalDateTime.of(2023, 1, 12, 17, 34, 9))
                                        .setName("RoleName-1")
                                        .build(),
                                TheAdministratorInput.TargetOf_roles
                                        .newBuilder()
                                        .setCreatedTime(LocalDateTime.of(2023, 1, 12, 16, 49, 27))
                                        .setModifiedTime(LocalDateTime.of(2023, 1, 12, 17, 34, 9))
                                        .setName("RoleName-2")
                                        .build()
                        )
                )
                .build();
        assertJson(
                "{" +
                        "--->\"id\":null," +
                        "--->\"name\":\"AdminName\"," +
                        "--->\"deleted\":false," +
                        "--->\"createdTime\":\"2023-01-12 16:49:27\"," +
                        "--->\"modifiedTime\":\"2023-01-12 17:34:09\"," +
                        "--->\"roles\":[" +
                        "--->--->{" +
                        "--->--->--->\"id\":null," +
                        "--->--->--->\"name\":\"RoleName-1\"," +
                        "--->--->--->\"deleted\":false," +
                        "--->--->--->\"createdTime\":\"2023-01-12 16:49:27\"," +
                        "--->--->--->\"modifiedTime\":\"2023-01-12 17:34:09\"" +
                        "--->--->},{" +
                        "--->--->--->\"id\":null," +
                        "--->--->--->\"name\":\"RoleName-2\"," +
                        "--->--->--->\"deleted\":false," +
                        "--->--->--->\"createdTime\":\"2023-01-12 16:49:27\"," +
                        "--->--->--->\"modifiedTime\":\"2023-01-12 17:34:09\"" +
                        "--->--->}" +
                        "--->]" +
                        "}",
                new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(input)
        );
        assertJson(
                "{" +
                        "--->\"name\":\"AdminName\"," +
                        "--->\"deleted\":false," +
                        "--->\"createdTime\":\"2023-01-12 16:49:27\"," +
                        "--->\"modifiedTime\":\"2023-01-12 17:34:09\"," +
                        "--->\"roles\":[" +
                        "--->--->{" +
                        "--->--->--->\"name\":\"RoleName-1\"," +
                        "--->--->--->\"deleted\":false," +
                        "--->--->--->\"createdTime\":\"2023-01-12 16:49:27\"," +
                        "--->--->--->\"modifiedTime\":\"2023-01-12 17:34:09\"" +
                        "--->--->},{" +
                        "--->--->--->\"name\":\"RoleName-2\"," +
                        "--->--->--->\"deleted\":false," +
                        "--->--->--->\"createdTime\":\"2023-01-12 16:49:27\"," +
                        "--->--->--->\"modifiedTime\":\"2023-01-12 17:34:09\"" +
                        "--->--->}" +
                        "--->]" +
                        "}",
                input.toEntity().toString()
        );
    }

    private static void assertJson(String expected, String actual) {
        Assertions.assertEquals(
                expected.replace("--->", "").replace("\r", "").replace("\n", ""),
                actual
        );
    }
}
