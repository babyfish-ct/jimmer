package org.babyfish.jimmer.json;

import java.lang.reflect.Type;

public interface JsonConverter {

    <T> T convert(Object value, Type targetType) throws Exception;

    default <T> T convert(Object value, Class<T> targetType) throws Exception {
        return convert(value, (Type) targetType);
    }

    default <T> T convert(Object value, TypeCreator creator) throws Exception {
        return convert(value, creator.createType(JsonTypeFactory.INSTANCE));
    }
}
