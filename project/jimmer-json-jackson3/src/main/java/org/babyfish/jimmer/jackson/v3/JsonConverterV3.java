package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.json.JsonConverter;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

public class JsonConverterV3 implements JsonConverter {
    private final ObjectMapper mapper;

    public JsonConverterV3(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <T> T convert(Object value, Type targetType) {
        return mapper.convertValue(value, mapper.getTypeFactory().constructType(targetType));
    }
}
