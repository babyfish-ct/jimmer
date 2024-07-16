package org.babyfish.jimmer.sql.cache.redisson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Book;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

public class InvalidateMessageTest extends Tests {

    @Test
    public void test() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(
                new InvalidateMessage(
                        UUID.fromString("462304f7-a9f6-41c7-b8fe-cb02e9fb3432"),
                        Book.class.getName(),
                        "authors",
                        InvalidateMessage.MAPPER.writeValueAsString(
                                Arrays.asList(
                                        Constants.graphQLInActionId1,
                                        Constants.graphQLInActionId2,
                                        Constants.graphQLInActionId3
                                )
                        )
                )
        );
        CacheTracker.InvalidationEvent event = mapper
                .readValue(json, InvalidateMessage.class)
                .toEvent();
        assertContentEquals(
                "org.babyfish.jimmer.sql.model.Book.authors[" +
                        "--->a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                        "--->e37a8344-73bb-4b23-ba76-82eac11f03e6, " +
                        "--->780bdf07-05af-48bf-9be9-f8c65236fecc" +
                        "]",
                event
        );
    }
}
