package org.babyfish.jimmer.json.codec;

public interface JsonConverter {
    <T> T convert(Object value, Class<T> targetType) throws Exception;

    <T> T convert(Object value, JsonType targetType) throws Exception;
}
