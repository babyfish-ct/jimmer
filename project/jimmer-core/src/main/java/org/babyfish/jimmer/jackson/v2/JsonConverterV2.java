package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.jackson.codec.JsonConverter;
import org.babyfish.jimmer.jackson.codec.JsonTypeFactory;
import org.babyfish.jimmer.jackson.codec.TypeCreator;

public class JsonConverterV2 implements JsonConverter {
    private final ObjectMapper mapper;
    private final JsonTypeFactory<JavaType> typeFactory;

    public JsonConverterV2(ObjectMapper mapper) {
        this.mapper = mapper;
        this.typeFactory = new JsonTypeFactoryV2(mapper.getTypeFactory());
    }

    @Override
    public <T> T convert(Object value, Class<T> targetType) throws Exception {
        return mapper.convertValue(value, targetType);
    }

    @Override
    public <T> T convert(Object value, TypeCreator typeCreator) throws Exception {
        return mapper.convertValue(value, (JavaType) typeCreator.createType(typeFactory));
    }
}
