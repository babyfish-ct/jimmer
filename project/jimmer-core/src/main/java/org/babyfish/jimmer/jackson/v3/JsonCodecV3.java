package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.jackson.codec.*;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.babyfish.jimmer.jackson.v3.ModulesRegistrarV3.registerWellKnownModules;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;
import static tools.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;

public class JsonCodecV3 implements JsonCodec<JavaType> {
    private final JsonMapper mapper;
    private final JsonTypeFactory<JavaType> typeFactory;
    private final JsonConverter converter;

    public JsonCodecV3() {
        this(createDefaultMapper());
    }

    public JsonCodecV3(JsonMapper mapper) {
        this.mapper = mapper;
        this.typeFactory = new JsonTypeFactoryV3(mapper.getTypeFactory());
        this.converter = new JsonConverterV3(mapper);
    }

    private static JsonMapper createDefaultMapper() {
        JsonMapper.Builder builder = JsonMapper.builder()
                .disable(FAIL_ON_TRAILING_TOKENS)
                .disable(SORT_PROPERTIES_ALPHABETICALLY);

        registerWellKnownModules(builder);

        return builder.build();
    }

    @Override
    public JsonCodec<JavaType> withCustomizations(JsonCodecCustomization... customizations) {
        JsonMapper.Builder builder = mapper.rebuild();
        for (JsonCodecCustomization c : customizations) {
            c.customizeV3(builder);
        }
        return new JsonCodecV3(builder.build());
    }

    @Override
    public JsonConverter converter() {
        return converter;
    }

    @Override
    public <T> JsonReader<T> readerFor(Class<T> clazz) {
        return new JsonReaderV3<>(mapper.readerFor(clazz));
    }

    @Override
    public <T> JsonReader<T> readerFor(TypeCreator<JavaType> typeCreator) {
        return new JsonReaderV3<>(mapper.readerFor(typeCreator.createType(typeFactory)));
    }

    @Override
    public <T> JsonReader<T[]> readerForArrayOf(Class<T> componentType) {
        return new JsonReaderV3<>(mapper.readerForArrayOf(componentType));
    }

    @Override
    public <T> JsonReader<List<T>> readerForListOf(Class<T> elementType) {
        return new JsonReaderV3<>(mapper.readerForListOf(elementType));
    }

    @Override
    public <V> JsonReader<Map<String, V>> readerForMapOf(Class<V> valueType) {
        return new JsonReaderV3<>(mapper.readerForMapOf(valueType));
    }

    @Override
    public JsonReader<Node> treeReader() {
        return new MappingJsonReader<>(new JsonReaderV3<>(mapper.readerFor(JsonNode.class)), NodeV3::new);
    }

    @Override
    public JsonWriter writer() {
        return new JsonWriterV3(mapper.writer());
    }

    @Override
    public JsonWriter writerFor(Class<?> clazz) {
        return new JsonWriterV3(mapper.writerFor(clazz));
    }

    @Override
    public JsonWriter writerFor(TypeCreator<JavaType> typeCreator) {
        return new JsonWriterV3(mapper.writerFor(typeCreator.createType(typeFactory)));
    }

    @Override
    public JacksonVersion version() {
        return JacksonVersion.V3;
    }
}
