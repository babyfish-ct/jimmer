package org.babyfish.jimmer.json.jackson.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.babyfish.jimmer.json.codec.*;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.babyfish.jimmer.json.jackson.v2.ModulesRegistrarV2.registerWellKnownModules;

public class JsonCodecV2 implements JsonCodec {
    private final ObjectMapper mapper;
    private final JsonConverter converter;
    private final JacksonTypeFactoryV2 typeFactory;

    public JsonCodecV2() {
        this(createDefaultMapper());
    }

    public JsonCodecV2(ObjectMapper mapper) {
        this.mapper = mapper;
        this.converter = new JsonConverterV2(mapper);
        this.typeFactory = new JacksonTypeFactoryV2(mapper.getTypeFactory());
    }

    private static ObjectMapper createDefaultMapper() {
        JsonMapper.Builder builder = JsonMapper.builder()
                .disable(WRITE_DATES_AS_TIMESTAMPS)
                .disable(FAIL_ON_UNKNOWN_PROPERTIES);

        registerWellKnownModules(builder);

        return builder.build();
    }

    @Override
    public JsonCodec withCustomizations(JsonCodecCustomization... customizations) {
        ObjectMapperBuilder builder = new ObjectMapperBuilder(mapper.copy());
        JsonCodecCustomizationTargetV2 target = new JsonCodecCustomizationTargetV2(builder);
        for (JsonCodecCustomization c : customizations) {
            c.customize(target);
        }
        return new JsonCodecV2(builder.build());
    }

    @Override
    public JsonConverter converter() {
        return converter;
    }

    @Override
    public <T> JsonReader<T> readerFor(JsonType type) {
        return new JsonReaderV2<>(mapper.readerFor(typeFactory.javaType(type)));
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
    public JsonWriter writerFor(JsonType type) {
        return new JsonWriterV2(mapper.writerFor(typeFactory.javaType(type)));
    }
}
