package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.dto.BookInput;
import org.babyfish.jimmer.sql.model.dto.BookInput2;
import org.babyfish.jimmer.sql.model.dto.BookInput3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

public class BookInputTest extends Tests {

    @Test
    public void testBookInput() {
        BookInput input = new BookInput();
        input.setId(UUID.fromString("66eee693-4b80-4d28-98b9-5e3aafca8d05"));
        input.setName("SQL in Action");
        input.setEdition(1);
        input.setPrice(new BigDecimal("47.3"));
        input.setStoreId(UUID.fromString("a977232c-c46e-43f5-98c5-5e5eb0a31027"));
        input.setAuthorIds(
                Arrays.asList(
                        UUID.fromString("b71100ba-1ffd-4d3a-9fc0-14b1b0a394c2"),
                        UUID.fromString("457b3804-448a-4288-833d-67f171f11aea")
                )
        );
        Book book = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"id\":\"66eee693-4b80-4d28-98b9-5e3aafca8d05\"," +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"edition\":1,\"price\":47.3," +
                        "--->\"store\":{\"id\":\"a977232c-c46e-43f5-98c5-5e5eb0a31027\"}," +
                        "--->\"authors\":[" +
                        "--->--->{\"id\":\"b71100ba-1ffd-4d3a-9fc0-14b1b0a394c2\"}," +
                        "--->--->{\"id\":\"457b3804-448a-4288-833d-67f171f11aea\"}" +
                        "--->]" +
                        "}",
                book
        );
        Assertions.assertEquals(input, new BookInput(book));
    }

    @Test
    public void testBookInput2() {
        BookInput2 input = new BookInput2();
        input.setId(UUID.fromString("66eee693-4b80-4d28-98b9-5e3aafca8d05"));
        input.setName("SQL in Action");
        input.setEdition(1);
        input.setPrice(new BigDecimal("47.3"));
        input.setStoreId(UUID.fromString("a977232c-c46e-43f5-98c5-5e5eb0a31027"));
        input.setAuthorIds(
                Arrays.asList(
                        UUID.fromString("b71100ba-1ffd-4d3a-9fc0-14b1b0a394c2"),
                        UUID.fromString("457b3804-448a-4288-833d-67f171f11aea")
                )
        );
        Book book = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"id\":\"66eee693-4b80-4d28-98b9-5e3aafca8d05\"," +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"edition\":1,\"price\":47.3," +
                        "--->\"store\":{\"id\":\"a977232c-c46e-43f5-98c5-5e5eb0a31027\"}," +
                        "--->\"authors\":[" +
                        "--->--->{\"id\":\"b71100ba-1ffd-4d3a-9fc0-14b1b0a394c2\"}," +
                        "--->--->{\"id\":\"457b3804-448a-4288-833d-67f171f11aea\"}" +
                        "--->]" +
                        "}",
                book
        );
        Assertions.assertEquals(input, new BookInput2(book));
    }

    @Test
    public void testBookInput3() {
        BookInput3 input = new BookInput3();
        input.setId(UUID.fromString("66eee693-4b80-4d28-98b9-5e3aafca8d05"));
        input.setName("SQL in Action");
        input.setEdition(1);
        input.setPrice(new BigDecimal("47.3"));
        input.setParentId(UUID.fromString("a977232c-c46e-43f5-98c5-5e5eb0a31027"));
        input.setAuthorIds(
                Arrays.asList(
                        UUID.fromString("b71100ba-1ffd-4d3a-9fc0-14b1b0a394c2"),
                        UUID.fromString("457b3804-448a-4288-833d-67f171f11aea")
                )
        );
        Book book = input.toEntity();
        assertContentEquals(
                "{" +
                        "--->\"id\":\"66eee693-4b80-4d28-98b9-5e3aafca8d05\"," +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"edition\":1,\"price\":47.3," +
                        "--->\"store\":{\"id\":\"a977232c-c46e-43f5-98c5-5e5eb0a31027\"}," +
                        "--->\"authors\":[" +
                        "--->--->{\"id\":\"b71100ba-1ffd-4d3a-9fc0-14b1b0a394c2\"}," +
                        "--->--->{\"id\":\"457b3804-448a-4288-833d-67f171f11aea\"}" +
                        "--->]" +
                        "}",
                book
        );
        Assertions.assertEquals(input, new BookInput3(book));
    }
}
