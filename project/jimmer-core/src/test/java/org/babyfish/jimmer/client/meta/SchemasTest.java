package org.babyfish.jimmer.client.meta;

import org.babyfish.jimmer.client.meta.impl.ApiOperationImpl;
import org.babyfish.jimmer.client.meta.impl.Schemas;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.jackson.v3.JsonCodecV3;
import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.json.Node;
import org.babyfish.jimmer.json.fastjson2.Fastjson2Codec;
import org.babyfish.jimmer.json.gson.GsonCodec;
import org.babyfish.jimmer.support.JacksonBackend;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SchemasTest {

    @Test
    public void roundTripLegacyFormat() throws IOException {
        String fixture = fixture();
        Schema schema = Schemas.readFrom(new StringReader(fixture));
        ApiService service = schema.getApiServiceMap().get(TypeName.parse("example.Catalog"));
        assertEquals("目录", service.getDoc().getValue());
        ApiOperation operation = service.getOperations().get(0);
        assertEquals("list:long:java.lang.String", ((ApiOperationImpl<?>) operation).key());
        assertEquals(1, operation.getParameters().get(0).getOriginalIndex());
        assertEquals("example.Errors$Missing", operation.getExceptionTypes().get(0).getTypeName().toString(true));
        assertEquals(TypeDefinition.Kind.OBJECT, schema.getTypeDefinitionMap().get(TypeName.parse("example.Errors$Missing")).getKind());

        StringWriter writer = new StringWriter();
        Schemas.writeTo(schema, writer);
        assertEquals(fixture.replaceAll("\\s+", ""), writer.toString().replaceAll("\\s+", ""));
    }

    @Test
    public void groupSelection() throws IOException {
        Schema schema = Schemas.readFrom(new StringReader(fixture()), Collections.singleton("web"));
        assertEquals(2, schema.getApiServiceMap().size());
        ApiService catalog = schema.getApiServiceMap().get(TypeName.parse("example.Catalog"));
        assertEquals(1, catalog.getOperations().size());
        assertEquals("list", catalog.getOperations().get(0).getName());
        assertEquals(2, schema.getTypeDefinitionMap().size());
        assertFalse(schema.getTypeDefinitionMap().containsKey(TypeName.parse("example.Status")));
    }

    @Test
    public void servicesOnly() throws IOException {
        Schema schema = Schemas.readServicesFrom(new StringReader("{\"services\":[{\"typeName\":\"example.Ping\"}],\"definitions\":[{}]}"));
        assertEquals(1, schema.getApiServiceMap().size());
        assertTrue(schema.getTypeDefinitionMap().isEmpty());
    }

    @Test
    public void adaptersReadEachOthersSchemas() throws Exception {
        String fixture = fixture();
        Schema schema = Schemas.readFrom(new StringReader(fixture));
        Node expected = JsonCodec.jsonCodec().treeReader().read(fixture);
        List<JsonCodec> codecs = Arrays.asList(
                new JsonCodecV2(), new JsonCodecV3(), new GsonCodec(), new Fastjson2Codec()
        );
        for (JsonCodec writerCodec : codecs) {
            OpenWriter writer = new OpenWriter();
            Schemas.writeTo(schema, writer, writerCodec);
            assertTrue(writer.flushed);
            String json = writer.toString();
            for (JsonCodec reader : codecs) {
                assertTrue(expected.contentEquals(reader.treeReader().read(json)),
                        writerCodec.getClass().getSimpleName() + " -> " + reader.getClass().getSimpleName());
                Schema selected = Schemas.readFrom(new OpenReader(json), Collections.singleton("web"), reader);
                assertEquals(2, selected.getTypeDefinitionMap().size());
                assertEquals(1, selected.getApiServiceMap().get(TypeName.parse("example.Catalog")).getOperations().size());
                Schema services = Schemas.readServicesFrom(new OpenReader(json), reader);
                assertEquals(2, services.getApiServiceMap().size());
                assertTrue(services.getTypeDefinitionMap().isEmpty());
            }
        }
    }

    @Test
    public void applicationMappingSettingsDoNotChangeSchemaFields() throws Exception {
        String fixture = fixture();
        Node expected = JsonCodec.jsonCodec().treeReader().read(fixture);
        JsonCodec custom = new JsonCodecV2(JsonMapper.builder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build());
        for (JsonCodec codec : Arrays.asList(custom, JacksonBackend.JACKSON2.polymorphicCodec(),
                JacksonBackend.JACKSON3.polymorphicCodec())) {
            Schema schema = Schemas.readFrom(new OpenReader(fixture), null, codec);
            OpenWriter writer = new OpenWriter();
            Schemas.writeTo(schema, writer, codec);
            assertTrue(expected.contentEquals(codec.treeReader().read(writer.toString())));
        }
    }

    private static final class OpenReader extends StringReader {
        OpenReader(String value) { super(value); }

        @Override
        public void close() { fail("Schema reader closed its caller's input"); }
    }

    private static final class OpenWriter extends StringWriter {
        boolean flushed;

        @Override
        public void flush() { flushed = true; }

        @Override
        public void close() { fail("Schema writer closed its caller's output"); }
    }

    private static String fixture() throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                SchemasTest.class.getResourceAsStream("schema.json"), StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        }
        return text.toString();
    }
}
