package org.babyfish.jimmer.sql.cache.redisson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.jackson.v3.JsonCodecV3;
import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.json.fastjson2.Fastjson2Codec;
import org.babyfish.jimmer.json.gson.GsonCodec;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.pg.JsonWrapper;
import org.junit.jupiter.api.Test;
import org.redisson.client.codec.Codec;
import org.redisson.codec.TypedJsonJacksonCodec;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.babyfish.jimmer.json.JsonCodec.jsonCodec;
import static org.junit.jupiter.api.Assertions.*;

public class InvalidateMessageTest extends Tests {

    @Test
    public void heterogeneousTopicsRestoreExactIdTypeAndValue() throws Exception {
        long id = 9_007_199_254_740_993L;
        ImmutableType type = ImmutableType.get(JsonWrapper.class);
        InvalidateMessage message = new InvalidateMessage(
                UUID.randomUUID(), new CacheTracker.InvalidateEvent(type, Collections.singletonList(id))
        );
        JsonCodec[] codecs = {new JsonCodecV2(), new JsonCodecV3(), new GsonCodec(), new Fastjson2Codec()};
        for (JsonCodec writer : codecs) {
            Codec source = new InvalidateMessageCodec(writer);
            ByteBuf bytes = source.getValueEncoder().encode(message);
            try {
                for (JsonCodec reader : codecs) {
                    Codec target = new InvalidateMessageCodec(reader);
                    InvalidateMessage received = (InvalidateMessage) target.getValueDecoder().decode(bytes.duplicate(), null);
                    assertEquals(message.trackerId, received.trackerId);
                    assertEquals(message.ids, received.toEvent().getIds());
                }
            } finally {
                bytes.release();
            }
        }
    }

    @Test
    public void typedRedissonMapperKeepsItsIdFormatAndExplicitNull() throws Exception {
        SimpleModule module = new SimpleModule();
        module.addSerializer(UUID.class, new JsonSerializer<UUID>() {
            @Override
            public void serialize(UUID id, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString("id:" + id);
            }
        });
        module.addDeserializer(UUID.class, new JsonDeserializer<UUID>() {
            @Override
            public UUID deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
                return UUID.fromString(parser.getText().substring(3));
            }
        });
        ObjectMapper mapper = new ObjectMapper().registerModule(module);
        Codec topicCodec = RedissonCacheTracker.topicCodec(new TypedJsonJacksonCodec(InvalidateMessage.class, mapper));
        InvalidateMessage original = new InvalidateMessage(
                UUID.randomUUID(), new CacheTracker.InvalidateEvent(
                        BookProps.ID.unwrap().getDeclaringType(), Collections.singletonList(Constants.graphQLInActionId1)
                )
        );
        String wire = "{\"trackerId\":\"" + original.trackerId + "\",\"typeName\":\"" + original.typeName +
                "\",\"propName\":null,\"ids\":[\"id:" + Constants.graphQLInActionId1 + "\"]}";
        ByteBuf encoded = topicCodec.getValueEncoder().encode(original);
        try {
            assertEquals(mapper.readTree(wire), mapper.readTree(encoded.toString(StandardCharsets.UTF_8)));
        } finally {
            encoded.release();
        }
        ByteBuf received = Unpooled.copiedBuffer(wire, StandardCharsets.UTF_8);
        try {
            InvalidateMessage decoded = (InvalidateMessage) topicCodec.getValueDecoder().decode(received, null);
            assertEquals(original.trackerId, decoded.trackerId);
            assertEquals(original.ids, decoded.toEvent().getIds());
            assertNull(decoded.propName);
        } finally {
            received.release();
        }
    }

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
    public void testTypeByJacksonForIssue621() throws Exception {
        String json = jsonCodec().writer().writeAsString(
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

        CacheTracker.InvalidateEvent event = jsonCodec()
                .readerFor(InvalidateMessage.class)
                .read(json)
                .toEvent();

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
    public void testPropByJacksonForIssue621() throws Exception {
        String json = jsonCodec().writer().writeAsString(
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

        CacheTracker.InvalidateEvent event = jsonCodec()
                .readerFor(InvalidateMessage.class)
                .read(json)
                .toEvent();

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
