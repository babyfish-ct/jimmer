package org.babyfish.jimmer.sql.cache.redisson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.BookProps;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.UUID;

public class InvalidateMessageTest extends Tests {

    @Test
    public void testType() throws IOException, ClassNotFoundException {
        byte[] arr;
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(
                    new InvalidateMessage(
                            UUID.randomUUID(),
                            new CacheTracker.InvalidateEvent(
                                    BookProps.ID.unwrap().getDeclaringType(),
                                    Arrays.asList(
                                            Constants.graphQLInActionId1,
                                            Constants.graphQLInActionId2,
                                            Constants.graphQLInActionId3
                                    )
                            )
                    )
            );
            out.flush();
            arr = bout.toByteArray();
        }

        CacheTracker.InvalidateEvent event;
        try (ObjectInputStream reader = new ObjectInputStream(new ByteArrayInputStream(arr))) {
            InvalidateMessage message = (InvalidateMessage) reader.readObject();
            event = message.toEvent();
        }
        assertContentEquals(
                "org.babyfish.jimmer.sql.model.Book[" +
                        "--->a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                        "--->e37a8344-73bb-4b23-ba76-82eac11f03e6, " +
                        "--->780bdf07-05af-48bf-9be9-f8c65236fecc" +
                        "]",
                event.toString()
        );
    }

    @Test
    public void testProp() throws IOException, ClassNotFoundException {
        byte[] arr;
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(
                    new InvalidateMessage(
                            UUID.randomUUID(),
                            new CacheTracker.InvalidateEvent(
                                    BookProps.AUTHORS.unwrap(),
                                    Arrays.asList(
                                            Constants.graphQLInActionId1,
                                            Constants.graphQLInActionId2,
                                            Constants.graphQLInActionId3
                                    )
                            )
                    )
            );
            out.flush();
            arr = bout.toByteArray();
        }

        CacheTracker.InvalidateEvent event;
        try (ObjectInputStream reader = new ObjectInputStream(new ByteArrayInputStream(arr))) {
            InvalidateMessage message = (InvalidateMessage) reader.readObject();
            event = message.toEvent();
        }
        assertContentEquals(
                "org.babyfish.jimmer.sql.model.Book.authors[" +
                        "--->a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                        "--->e37a8344-73bb-4b23-ba76-82eac11f03e6, " +
                        "--->780bdf07-05af-48bf-9be9-f8c65236fecc" +
                        "]",
                event.toString()
        );
    }

    @Test
    public void testTypeByJacksonForIssue621() throws JsonProcessingException {
        String json = new ObjectMapper().writeValueAsString(
                new InvalidateMessage(
                        UUID.randomUUID(),
                        new CacheTracker.InvalidateEvent(
                                BookProps.ID.unwrap().getDeclaringType(),
                                Arrays.asList(
                                        Constants.graphQLInActionId1,
                                        Constants.graphQLInActionId2,
                                        Constants.graphQLInActionId3
                                )
                        )
                )
        );

        CacheTracker.InvalidateEvent event = new ObjectMapper().readValue(
                json, InvalidateMessage.class
        ).toEvent();
        assertContentEquals(
                "org.babyfish.jimmer.sql.model.Book[" +
                        "--->a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                        "--->e37a8344-73bb-4b23-ba76-82eac11f03e6, " +
                        "--->780bdf07-05af-48bf-9be9-f8c65236fecc" +
                        "]",
                event.toString()
        );
    }

    @Test
    public void testPropByJacksonForIssue621() throws JsonProcessingException {
        String json = new ObjectMapper().writeValueAsString(
                new InvalidateMessage(
                        UUID.randomUUID(),
                        new CacheTracker.InvalidateEvent(
                                BookProps.AUTHORS.unwrap(),
                                Arrays.asList(
                                        Constants.graphQLInActionId1,
                                        Constants.graphQLInActionId2,
                                        Constants.graphQLInActionId3
                                )
                        )
                )
        );

        CacheTracker.InvalidateEvent event = new ObjectMapper()
                .readValue(json, InvalidateMessage.class
        ).toEvent();
        assertContentEquals(
                "org.babyfish.jimmer.sql.model.Book.authors[" +
                        "--->a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                        "--->e37a8344-73bb-4b23-ba76-82eac11f03e6, " +
                        "--->780bdf07-05af-48bf-9be9-f8c65236fecc" +
                        "]",
                event.toString()
        );
    }
}
