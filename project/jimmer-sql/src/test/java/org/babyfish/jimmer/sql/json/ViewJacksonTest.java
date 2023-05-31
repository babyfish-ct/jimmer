package org.babyfish.jimmer.sql.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.sql.model.Book;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ViewJacksonTest {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().registerModule(new ImmutableModule());

    @Test
    public void assertRaw() throws JsonProcessingException {
        String json = "{" +
                "\"store\":{\"id\":\"916e4629-f18f-49cf-9c0a-c161383d3939\"}," +
                "\"authors\":[" +
                "{\"id\":\"a3a529b5-b310-4af1-883d-9a4e0114653c\"}," +
                "{\"id\":\"1639d9d5-7b92-43cf-a03f-25314832f794\"}" +
                "]" +
                "}";
        Book book = MAPPER.readValue(json, Book.class);
        Assertions.assertEquals(
                json,
                book.toString()
        );
    }

    @Test
    public void assertView() throws JsonProcessingException {
        String json = "{" +
                "\"storeId\":\"916e4629-f18f-49cf-9c0a-c161383d3939\"," +
                "\"authorIds\":[" +
                "\"a3a529b5-b310-4af1-883d-9a4e0114653c\"," +
                "\"1639d9d5-7b92-43cf-a03f-25314832f794\"" +
                "]" +
                "}";
        Book book = MAPPER.readValue(json, Book.class);
        Assertions.assertEquals(
                json,
                book.toString()
        );
    }
}
