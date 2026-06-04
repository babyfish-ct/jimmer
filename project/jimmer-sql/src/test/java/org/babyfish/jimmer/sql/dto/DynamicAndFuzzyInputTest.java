package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.dto.DynamicBookInput;
import org.babyfish.jimmer.sql.model.dto.DynamicBookInput2;
import org.babyfish.jimmer.sql.model.dto.DynamicBookStoreInput;
import org.babyfish.jimmer.sql.model.dto.FuzzyBookInput;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;

public class DynamicAndFuzzyInputTest extends Tests {

    @Test
    public void testDynamicBookInput() throws Exception {
        DynamicBookInput input = new DynamicBookInput();
        input.setName("SQL in Action");
        String json = "{\"name\":\"SQL in Action\"}";
        assertContentEquals(
                json,
                input.toEntity()
        );
        Book book = jsonCodec().readerFor(DynamicBookInput.class)
                .read(json)
                .toEntity();
        assertContentEquals(
                json,
                book
        );
    }

    @Test
    public void testDynamicBookInput2() throws Exception {
        DynamicBookInput2 input = new DynamicBookInput2();
        input.setParentName("MANNING");
        assertContentEquals("DynamicBookInput2(parentName=MANNING)", input);
        Book book = jsonCodec()
                .readerFor(DynamicBookInput2.class)
                .read("{\"parentName\":\"MANNING\"}")
                .toEntity();
        assertContentEquals(
                "{\"store\":{\"name\":\"MANNING\"}}",
                book
        );
    }

    @Test
    public void testDynamicBookStoreInput() throws Exception {
        DynamicBookStoreInput input = new DynamicBookStoreInput();
        input.setName("MANNING");
        String json = "{\"name\":\"MANNING\"}";
        assertContentEquals(
                json,
                input.toEntity()
        );
        Book book = jsonCodec()
                .readerFor(DynamicBookInput.class)
                .read(json)
                .toEntity();
        assertContentEquals(
                json,
                book
        );
    }

    @Test
    public void testIssue994() throws Exception {
        DynamicBookInput input = new DynamicBookInput();
        input.setName("MANNING");
        String json = jsonCodec().writer().writeAsString(input);
        assertContentEquals(
                "{\"name\":\"MANNING\"}",
                json
        );
        Book book = jsonCodec()
                .readerFor(DynamicBookInput.class)
                .read(json)
                .toEntity();
        assertContentEquals(
                "{\"name\":\"MANNING\"}",
                book
        );
    }

    @Test
    public void testFuzzyInput() throws Exception {
        FuzzyBookInput input = new FuzzyBookInput();
        input.setName("SQL in Action");
        String json = jsonCodec().writer().writeAsString(input);
        String fuzzyJson = "{" +
                "\"name\":\"SQL in Action\"," +
                "\"edition\":null," +
                "\"price\":null," +
                "\"storeId\":null," +
                "\"authorIds\":null" +
                "}";
        assertContentEquals(fuzzyJson, json);
        assertContentEquals(
                "{\"name\":\"SQL in Action\"}",
                input.toEntity().toString()
        );
        Book book = jsonCodec()
                .readerFor(FuzzyBookInput.class)
                .read(fuzzyJson)
                .toEntity();
        assertContentEquals(
                "{\"name\":\"SQL in Action\"}",
                book
        );
    }
}
