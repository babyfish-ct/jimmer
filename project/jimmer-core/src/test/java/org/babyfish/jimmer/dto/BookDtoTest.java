package org.babyfish.jimmer.dto;

import org.babyfish.jimmer.model.Book;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class BookDtoTest {

    @Test
    public void testWithName() {
        BookDto bookDto = new BookDto(
                "SQL in Action",
                "1",
                "32",
                new BookDto.TargetOf_store(
                        "TURING"
                ),
                Arrays.asList(
                        new BookDto.TargetOf_authors("Jim"),
                        new BookDto.TargetOf_authors("Linda")
                )
        );
        Book book = bookDto.toEntity();
        assertContent(
                "{" +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"store\":{\"name\":\"TURING\"}," +
                        "--->\"price\":32,\"authors\":[" +
                        "--->--->{\"name\":\"Jim\"}," +
                        "--->--->{\"name\":\"Linda\"}" +
                        "--->]" +
                        "}",
                book
        );
    }

    @Test
    public void testWithoutName() {
        BookDto bookDto = new BookDto(
                null,
                "1",
                "32",
                new BookDto.TargetOf_store(
                        "TURING"
                ),
                Arrays.asList(
                        new BookDto.TargetOf_authors("Jim"),
                        new BookDto.TargetOf_authors("Linda")
                )
        );
        Book book = bookDto.toEntity();
        assertContent(
                "{" +
                        "--->\"store\":{\"name\":\"TURING\"}," +
                        "--->\"price\":32,\"authors\":[" +
                        "--->--->{\"name\":\"Jim\"}," +
                        "--->--->{\"name\":\"Linda\"}" +
                        "--->]" +
                        "}",
                book
        );
    }

    private static void assertContent(String content, Object o) {
        Assertions.assertEquals(content.replace("--->", ""), o.toString());
    }
}
