package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.json.codec.JsonConverter;
import org.babyfish.jimmer.json.codec.JsonType;

public class JsonConverterV2 implements JsonConverter {
    private final ObjectMapper mapper;

    public JsonConverterV2(ObjectMapper mapper) {
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
