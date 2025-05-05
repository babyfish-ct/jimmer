package org.babyfish.jimmer.sql.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.dto.DynamicBookInput;
import org.babyfish.jimmer.sql.model.dto.DynamicBookInput2;
import org.babyfish.jimmer.sql.model.dto.DynamicBookStoreInput;
import org.babyfish.jimmer.sql.model.dto.FuzzyBookInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DynamicAndFuzzyInputTest extends Tests {

    @Test
    public void testDynamicBookInput() throws JsonProcessingException {
        DynamicBookInput input = new DynamicBookInput();
        input.setName("SQL in Action");
        String json = "{\"name\":\"SQL in Action\"}";
        assertContentEquals(
                json,
                input.toEntity()
        );
        Book book = new ObjectMapper().readValue(json, DynamicBookInput.class)
                .toEntity();
        assertContentEquals(
                json,
                book
        );
    }

    @Test
    public void testDynamicBookInput2() throws JsonProcessingException {
        DynamicBookInput2 input = new DynamicBookInput2();
        input.setParentName("MANNING");
        assertContentEquals("DynamicBookInput2(parentName=MANNING)", input);
        Book book = new ObjectMapper().readValue("{\"parentName\":\"MANNING\"}", DynamicBookInput2.class)
                .toEntity();
        assertContentEquals(
                "{\"store\":{\"name\":\"MANNING\"}}",
                book
        );
    }

    @Test
    public void testDynamicBookStoreInput() throws JsonProcessingException {
        DynamicBookStoreInput input = new DynamicBookStoreInput();
        input.setName("MANNING");
        String json = "{\"name\":\"MANNING\"}";
        assertContentEquals(
                json,
                input.toEntity()
        );
        Book book = new ObjectMapper().readValue(json, DynamicBookInput.class)
                .toEntity();
        assertContentEquals(
                json,
                book
        );
    }

    @Test
    public void testIssue994() throws JsonProcessingException {
        DynamicBookInput input = new DynamicBookInput();
        input.setName("MANNING");
        String json = new ObjectMapper().writeValueAsString(input);
        assertContentEquals(
                "{\"name\":\"MANNING\"}",
                json
        );
        Book book = new ObjectMapper().readValue(json, DynamicBookInput.class)
                .toEntity();
        assertContentEquals(
                "{\"name\":\"MANNING\"}",
                book
        );
    }

    @Test
    public void testFuzzyInput() throws JsonProcessingException {
        FuzzyBookInput input = new FuzzyBookInput();
        input.setName("SQL in Action");
        String json = new ObjectMapper().writeValueAsString(input);
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
        Book book = new ObjectMapper().readValue(fuzzyJson, FuzzyBookInput.class)
                .toEntity();
        assertContentEquals(
                "{\"name\":\"SQL in Action\"}",
                book
        );
    }
}
