package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.babyfish.jimmer.jackson.codec.*;

import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.babyfish.jimmer.jackson.v2.ModulesRegistrarV2.registerWellKnownModules;

public class JsonCodecV2 implements JsonCodec<JavaType> {
    private final ObjectMapper mapper;
    private final JsonTypeFactory<JavaType> typeFactory;
    private final JsonConverter converter;

    public JsonCodecV2() {
        this(createDefaultMapper());
    }

    public JsonCodecV2(ObjectMapper mapper) {
        this.mapper = mapper;
        this.typeFactory = new JsonTypeFactoryV2(mapper.getTypeFactory());
        this.converter = new JsonConverterV2(mapper);
    }

    private static ObjectMapper createDefaultMapper() {
        JsonMapper.Builder builder = JsonMapper.builder()
                .disable(WRITE_DATES_AS_TIMESTAMPS)
                .disable(FAIL_ON_UNKNOWN_PROPERTIES);

        registerWellKnownModules(builder);

        return builder.build();
    }

    @Override
    public JsonCodec<JavaType> withCustomizations(JsonCodecCustomization... customizations) {
        ObjectMapperBuilder builder = new ObjectMapperBuilder(mapper.copy());
        for (JsonCodecCustomization c : customizations) {
            c.customizeV2(builder);
        }
        return new JsonCodecV2(builder.build());
    }

    @Override
    public JsonConverter converter() {
        return converter;
    }

    @Override
    public <T> JsonReader<T> readerFor(Class<T> clazz) {
        return new JsonReaderV2<>(mapper.readerFor(clazz));
    }

    @Override
    public <T> JsonReader<T> readerFor(TypeCreator<JavaType> typeCreator) {
        return new JsonReaderV2<>(mapper.readerFor(typeCreator.createType(typeFactory)));
    }

    @Override
    public <T> JsonReader<T[]> readerForArrayOf(Class<T> componentType) {
        return new JsonReaderV2<>(mapper.readerForArrayOf(componentType));
    }

    @Override
    public <T> JsonReader<List<T>> readerForListOf(Class<T> elementType) {
        return new JsonReaderV2<>(mapper.readerForListOf(elementType));
    }

    @Override
    public <V> JsonReader<Map<String, V>> readerForMapOf(Class<V> valueType) {
        return new JsonReaderV2<>(mapper.readerForMapOf(valueType));
    }

    @Override
    public JsonReader<Node> treeReader() {
        return new MappingJsonReader<>(new JsonReaderV2<>(mapper.readerFor(JsonNode.class)), NodeV2::new);
    }

    @Override
    public JsonWriter writer() {
        return new JsonWriterV2(mapper.writer());
    }

    @Override
    public JsonWriter writerFor(Class<?> clazz) {
        return new JsonWriterV2(mapper.writerFor(clazz));
    }

    @Override
    public JsonWriter writerFor(TypeCreator<JavaType> typeCreator) {
        return new JsonWriterV2(mapper.writerFor(typeCreator.createType(typeFactory)));
    }

    @Override
    public JacksonVersion version() {
        return JacksonVersion.V2;
    }
}
