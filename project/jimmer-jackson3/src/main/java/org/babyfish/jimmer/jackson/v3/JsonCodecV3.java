package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.json.codec.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.babyfish.jimmer.jackson.v3.ModulesRegistrarV3.registerWellKnownModules;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;
import static tools.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;

public class JsonCodecV3 implements JsonCodec {
    private final JsonMapper mapper;
    private final JsonConverter converter;
    private final JacksonTypeFactoryV3 typeFactory;

    public JsonCodecV3() {
        this(createDefaultMapper());
    }

    public JsonCodecV3(JsonMapper mapper) {
        this.mapper = mapper;
        this.converter = new JsonConverterV3(mapper);
        this.typeFactory = new JacksonTypeFactoryV3(mapper.getTypeFactory());
    }

    private static JsonMapper createDefaultMapper() {
        JsonMapper.Builder builder = JsonMapper.builder()
                .disable(FAIL_ON_TRAILING_TOKENS)
                .disable(SORT_PROPERTIES_ALPHABETICALLY);

        registerWellKnownModules(builder);

        return builder.build();
    }

    @Override
    public JsonCodec withCustomizations(JsonCodecCustomization... customizations) {
        JsonMapper.Builder builder = mapper.rebuild();
        JsonCodecCustomizationTargetV3 target = new JsonCodecCustomizationTargetV3(builder);
        for (JsonCodecCustomization c : customizations) {
            c.customize(target);
        }
        return new JsonCodecV3(builder.build());
    }

    @Override
    public JsonConverter converter() {
        return converter;
    }

    @Override
    public <T> JsonReader<T> readerFor(JsonType type) {
        return new JsonReaderV3<>(mapper.readerFor(typeFactory.javaType(type)));
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
    public JsonWriter writerFor(JsonType type) {
        return new JsonWriterV3(mapper.writerFor(typeFactory.javaType(type)));
    }
}
