package org.babyfish.jimmer.json;

import com.google.gson.JsonParser;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientExhaustiveInput;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientImplicitCatchAllView;
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientExhaustiveInput;
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientImplicitCatchAllView;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientDiscriminatorInput;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientPatchInput;
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientDiscriminatorInput;
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.dto.KClientPatchInput;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.babyfish.jimmer.json.JsonTestProviders.CODECS;
import static org.junit.jupiter.api.Assertions.*;

public class GeneratedDtoTest {

    private static final String BRANCH = "{\"type\":\"ORG\",\"id\":9007199254740993,\"name\":\"Acme\",\"taxCode\":\"A\"}";

    public record JavaRequest(List<ClientExhaustiveInput> items) {}
    public record KotlinRequest(List<KClientExhaustiveInput> items) {}

    @Test
    public void testGeneratedInputsInRecords() throws Exception {
        for (Class<?> envelope : List.of(JavaRequest.class, KotlinRequest.class)) {
            String input = "{\"items\":[" + BRANCH + "]}";
            for (JsonCodec writer : CODECS) {
                String source = writer.getClass().getSimpleName();
                Object request = writer.readerFor(envelope).read(input);
                String json = writer.writerFor(envelope).writeAsString(request);
                assertEquals(JsonParser.parseString(input), JsonParser.parseString(json), source + " / " + envelope);
                for (JsonCodec reader : CODECS) {
                    assertEquals(request, reader.readerFor(envelope).read(json), source + " -> " + reader.getClass().getSimpleName());
                }
            }
        }
    }

    @Test
    public void testDefaultBranchDiscriminator() throws Exception {
        String json = "{\"type\":\"OTHER\",\"id\":1,\"name\":\"Acme\"}";
        for (JsonCodec codec : CODECS) {
            String provider = codec.getClass().getSimpleName();
            for (Class<?> type : List.of(ClientDiscriminatorInput.class, KClientDiscriminatorInput.class)) {
                Object value = codec.readerFor(type).read(json);
                assertEquals("Default", value.getClass().getSimpleName());
                assertEquals(JsonParser.parseString(json), JsonParser.parseString(codec.writerFor(type).writeAsString(value)), provider);
            }
            for (Class<?> type : List.of(ClientPatchInput.class, KClientPatchInput.class)) {
                Object value = codec.readerFor(type).read(json);
                String discriminator = JsonParser.parseString(codec.writerFor(type).writeAsString(value))
                        .getAsJsonObject().get("type").getAsString();
                assertEquals(type.getSimpleName() + "$Default", discriminator, provider);
            }
        }
    }

    @Test
    public void testDeclaredViewProjection() throws Exception {
        String json = "{\"id\":1,\"name\":\"Acme\",\"taxCode\":\"A\"}";
        String projection = "{\"id\":1,\"name\":\"Acme\"}";
        for (Class<?> view : List.of(ClientImplicitCatchAllView.class, KClientImplicitCatchAllView.class)) {
            Class<?> subtype = Class.forName(view.getName() + "$Organization");
            Type list = JsonTypeFactory.INSTANCE.constructListType(view);
            for (JsonCodec codec : CODECS) {
                String provider = codec.getClass().getSimpleName();
                Object value = codec.readerFor(subtype).read(json);
                assertEquals(JsonParser.parseString(json), JsonParser.parseString(codec.writer().writeAsString(value)), provider);
                assertEquals(JsonParser.parseString(projection), JsonParser.parseString(codec.writerFor(view).writeAsString(value)), provider);
                assertEquals(JsonParser.parseString("[" + projection + "]"),
                        JsonParser.parseString(codec.writerFor(list).writeAsString(List.of(value))), provider);
                assertThrows(Exception.class, () -> codec.readerFor(view).read(projection), provider);
            }
        }
    }
}
