package org.babyfish.jimmer.jackson.codec;

public interface JsonConverter {
    <T> T convert(Object value, Class<T> targetType) throws Exception;

    <T> T convert(Object value, TypeCreator typeCreator) throws Exception;
}
