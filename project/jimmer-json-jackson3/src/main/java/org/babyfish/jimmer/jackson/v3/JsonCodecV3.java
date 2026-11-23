package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.json.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;

import static org.babyfish.jimmer.jackson.v3.ModulesRegistrarV3.registerWellKnownModules;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;
import static tools.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;

public class JsonCodecV3 implements JsonCodec {
    private final JsonMapper mapper;
    private final JsonConverter converter;

    public JsonCodecV3() {
        this(createDefaultMapper());
    }

    public JsonCodecV3(JsonMapper mapper) {
        this.mapper = mapper;
        this.converter = new JsonConverterV3(mapper);
    }

    private static JsonMapper createDefaultMapper() {
        JsonMapper.Builder builder = JsonMapper.builder()
                .disable(FAIL_ON_TRAILING_TOKENS)
                .disable(SORT_PROPERTIES_ALPHABETICALLY);

        registerWellKnownModules(builder);
        ModulesRegistrarV3.registerImmutableModule(builder);

        return builder.build();
    }

    @Override
    public JsonConverter converter() {
        return converter;
    }

    @Override
    public <T> JsonReader<T> readerFor(Type type) {
        return new JsonReaderV3<>(mapper.readerFor(mapper.getTypeFactory().constructType(type)));
    }

    @Override
    public JsonReader<Node> treeReader() {
        return new MappingJsonReader<>(new JsonReaderV3<>(mapper.readerFor(JsonNode.class).with(tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)), NodeV3::new);
    }

    @Override
    public void writeTree(Writer target, Object tree) throws IOException {
        TreeWriter.INSTANCE.writeValue(target, tree);
    }

    private static class TreeWriter {
        static final tools.jackson.databind.ObjectWriter INSTANCE = JsonMapper.builder()
                .disable(tools.jackson.core.StreamWriteFeature.AUTO_CLOSE_TARGET)
                .build()
                .writerWithDefaultPrettyPrinter();
    }

    @Override
    public JsonWriter<Object> writer() {
        return new JsonWriterV3<>(mapper.writer());
    }

    @Override
    public <T> JsonWriter<T> writerFor(Type type) {
        return new JsonWriterV3<>(mapper.writerFor(mapper.getTypeFactory().constructType(type)));
    }
}
