package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.json.JsonConverter;

import java.lang.reflect.Type;

public class JsonConverterV2 implements JsonConverter {
    private final ObjectMapper mapper;

    public JsonConverterV2(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <T> T convert(Object value, Type targetType) {
        return mapper.convertValue(value, mapper.getTypeFactory().constructType(targetType));
    }
}
