package org.babyfish.jimmer.json.jackson.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.json.codec.JsonConverter;
import org.babyfish.jimmer.json.codec.JsonType;

public class JsonConverterV2 implements JsonConverter {
    private final ObjectMapper mapper;
    private final JacksonTypeFactoryV2 typeFactory;

    public JsonConverterV2(ObjectMapper mapper) {
        this.mapper = mapper;
        this.typeFactory = new JacksonTypeFactoryV2(mapper.getTypeFactory());
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
