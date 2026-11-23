package org.babyfish.jimmer.nojackson;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.impl.Schemas;
import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.json.JsonCodecs;
import org.babyfish.jimmer.json.fastjson2.Fastjson2Codec;
import org.babyfish.jimmer.json.gson.GsonCodec;
import org.babyfish.jimmer.nojackson.dto.KNoteInput;
import org.babyfish.jimmer.nojackson.dto.NoteInput;
import org.babyfish.jimmer.sql.JSqlClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class NoJacksonTest {

    static Stream<JsonCodec> codecs() {
        return Stream.of(new GsonCodec(), new Fastjson2Codec());
    }

    @Test
    public void testExplicitCodecWithoutJackson() {
        JsonCodec codec = JsonCodecs.create(CustomCodec.class.getName());
        JSqlClient client = JSqlClient.newBuilder().setJsonCodec(codec).build();
        assertSame(codec, client.getJsonCodec());
        String json = "{\"id\":42}";
        for (Class<?> model : new Class<?>[] { Note.class, KNote.class }) {
            Object entity = ImmutableObjects.fromString(model, json, client.getJsonCodec());
            assertEquals(json, ImmutableObjects.toString(entity, client.getJsonCodec()));
            IllegalStateException ex = assertThrows(IllegalStateException.class, entity::toString);
            assertTrue(ex.getMessage().contains("Pass a JsonCodec explicitly"));
        }
    }

    public static class CustomCodec extends GsonCodec {}

    @ParameterizedTest
    @MethodSource("codecs")
    public void testGeneratedInputs(JsonCodec codec) throws Exception {
        assertDynamicInput(codec, NoteInput.class, Note.class);
        assertDynamicInput(codec, KNoteInput.class, KNote.class);
    }

    @ParameterizedTest
    @MethodSource("codecs")
    public void testGeneratedClientSchemas(JsonCodec codec) throws Exception {
        Set<TypeName> services = new HashSet<>();
        Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/jimmer/client");
        for (URL resource : Collections.list(resources)) {
            try (Reader reader = new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)) {
                services.addAll(Schemas.readServicesFrom(reader, codec).getApiServiceMap().keySet());
            }
        }
        assertTrue(services.contains(TypeName.of(NoteApi.class)));
        assertTrue(services.contains(TypeName.of(KNoteApi.class)));
    }

    private static void assertDynamicInput(
            JsonCodec codec,
            Class<? extends Input<?>> type,
            Class<?> entityType
    ) throws Exception {
        Input<?> missing = codec.readerFor(type).read("{}");
        assertFalse(ImmutableObjects.isLoaded(missing.toEntity(), "label"));
        assertEquals("{}", codec.writerFor(type).writeAsString(missing));

        String explicitNull = "{\"label\":null}";
        Input<?> cleared = codec.readerFor(type).read(explicitNull);
        Object entity = cleared.toEntity();
        assertTrue(ImmutableObjects.isLoaded(entity, "label"));
        assertNull(ImmutableObjects.get(entity, "label"));
        assertEquals(explicitNull, codec.writerFor(type).writeAsString(cleared));
        assertEquals(explicitNull, ImmutableObjects.toString(entity, codec));
        Object restored = ImmutableObjects.fromString(entityType, explicitNull, codec);
        assertTrue(ImmutableObjects.isLoaded(restored, "label"));
        assertNull(ImmutableObjects.get(restored, "label"));
    }
}
