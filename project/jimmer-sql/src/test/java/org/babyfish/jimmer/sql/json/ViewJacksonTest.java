package org.babyfish.jimmer.sql.json;

import org.babyfish.jimmer.sql.model.Book;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;

public class ViewJacksonTest {

    @Test
    public void assertRaw() throws Exception {
        String json = "{" +
                "\"store\":{\"id\":\"916e4629-f18f-49cf-9c0a-c161383d3939\"}," +
                "\"authors\":[" +
                "{\"id\":\"a3a529b5-b310-4af1-883d-9a4e0114653c\"}," +
                "{\"id\":\"1639d9d5-7b92-43cf-a03f-25314832f794\"}" +
                "]" +
                "}";
        Book book2 = jsonCodec().readerFor(Book.class).read(json);
        Assertions.assertEquals(
                json,
                book2.toString()
        );
    }

    @Test
    public void assertView() throws Exception {
        String json = "{" +
                "\"storeId\":\"916e4629-f18f-49cf-9c0a-c161383d3939\"," +
                "\"authorIds\":[" +
                "\"a3a529b5-b310-4af1-883d-9a4e0114653c\"," +
                "\"1639d9d5-7b92-43cf-a03f-25314832f794\"" +
                "]" +
                "}";
        Book book = jsonCodec().readerFor(Book.class).read(json);
        Assertions.assertEquals(
                json,
                book.toString()
        );
    }
}
