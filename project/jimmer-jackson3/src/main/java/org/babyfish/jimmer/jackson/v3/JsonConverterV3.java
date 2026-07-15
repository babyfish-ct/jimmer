package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.json.codec.JsonConverter;
import org.babyfish.jimmer.json.codec.JsonType;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

public class JsonConverterV3 implements JsonConverter {
    private final ObjectMapper mapper;

    public JsonConverterV3(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <T> T convert(Object value, Class<T> targetType) throws Exception {
        return mapper.convertValue(value, targetType);
    }

    @Override
    public <T> T convert(Object value, JsonType targetType) throws Exception {
        return mapper.convertValue(value, javaType(targetType));
    }

    private JavaType javaType(JsonType type) {
        return mapper.getTypeFactory().constructType(type.getType());
    }
}
