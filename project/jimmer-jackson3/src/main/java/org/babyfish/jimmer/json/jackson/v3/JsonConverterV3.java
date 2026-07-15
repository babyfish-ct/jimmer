package org.babyfish.jimmer.json.jackson.v3;

import org.babyfish.jimmer.json.codec.JsonConverter;
import org.babyfish.jimmer.json.codec.JsonType;
import tools.jackson.databind.ObjectMapper;

public class JsonConverterV3 implements JsonConverter {
    private final ObjectMapper mapper;
    private final JacksonTypeFactoryV3 typeFactory;

    public JsonConverterV3(ObjectMapper mapper) {
        this.mapper = mapper;
        this.typeFactory = new JacksonTypeFactoryV3(mapper.getTypeFactory());
    }

    @Override
    public <T> T convert(Object value, Class<T> targetType) throws Exception {
        return mapper.convertValue(value, targetType);
    }

    @Override
    public <T> T convert(Object value, JsonType targetType) throws Exception {
        return mapper.convertValue(value, typeFactory.javaType(targetType));
    }
}
