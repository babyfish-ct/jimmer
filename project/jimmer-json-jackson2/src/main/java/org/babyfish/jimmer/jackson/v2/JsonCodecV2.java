package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.babyfish.jimmer.json.*;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.babyfish.jimmer.jackson.v2.ModulesRegistrarV2.registerWellKnownModules;

public class JsonCodecV2 implements JsonCodec {
    private final ObjectMapper mapper;
    private final JsonConverter converter;

    public JsonCodecV2() {
        this(createDefaultMapper());
    }

    public JsonCodecV2(ObjectMapper mapper) {
        this.mapper = mapper;
        this.converter = new JsonConverterV2(mapper);
    }

    private static ObjectMapper createDefaultMapper() {
        JsonMapper.Builder builder = JsonMapper.builder()
                .disable(WRITE_DATES_AS_TIMESTAMPS)
                .disable(FAIL_ON_UNKNOWN_PROPERTIES);

        registerWellKnownModules(builder);
        ModulesRegistrarV2.registerImmutableModule(builder);

        return builder.build();
    }

    @Override
    public JsonConverter converter() {
        return converter;
    }

    @Override
    public <T> JsonReader<T> readerFor(Type type) {
        return new JsonReaderV2<>(mapper.readerFor(mapper.getTypeFactory().constructType(type)));
    }

    @Override
    public JsonReader<Node> treeReader() {
        return new MappingJsonReader<>(new JsonReaderV2<>(mapper.readerFor(JsonNode.class).with(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)), NodeV2::new);
    }

    @Override
    public void writeTree(Writer target, Object tree) throws IOException {
        TreeWriter.INSTANCE.writeValue(target, tree);
    }

    private static class TreeWriter {
        static final com.fasterxml.jackson.databind.ObjectWriter INSTANCE = JsonMapper.builder()
                .disable(com.fasterxml.jackson.core.StreamWriteFeature.AUTO_CLOSE_TARGET)
                .build()
                .writerWithDefaultPrettyPrinter();
    }

    @Override
    public JsonWriter<Object> writer() {
        return new JsonWriterV2<>(mapper.writer());
    }

    @Override
    public <T> JsonWriter<T> writerFor(Type type) {
        return new JsonWriterV2<>(mapper.writerFor(mapper.getTypeFactory().constructType(type)));
    }
}
