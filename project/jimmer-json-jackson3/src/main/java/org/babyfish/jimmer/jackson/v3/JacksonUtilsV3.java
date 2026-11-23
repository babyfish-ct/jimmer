package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.ArrayType;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapType;
import tools.jackson.databind.type.SimpleType;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

class JacksonUtilsV3 {

    static JavaType getJacksonType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType == Collection.class || rawType == List.class || rawType == Set.class) {
                return CollectionType.construct(
                        (Class<?>) rawType,
                        null,
                        null,
                        null,
                        getJacksonType(parameterizedType.getActualTypeArguments()[0])
                );
            }
            if (rawType == Map.class) {
                return MapType.construct(
                        (Class<?>) rawType,
                        null,
                        null,
                        null,
                        getJacksonType(parameterizedType.getActualTypeArguments()[0]),
                        getJacksonType(parameterizedType.getActualTypeArguments()[1])
                );
            }
            throw new IllegalArgumentException("Parameterized type must be collection, list, set or map");
        }
        if (type instanceof TypeVariable<?>) {
            return getJacksonType(((TypeVariable<?>) type).getBounds()[0]);
        }
        if (type instanceof WildcardType) {
            return getJacksonType(((WildcardType) type).getUpperBounds()[0]);
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType arrType = (GenericArrayType) type;
            return ArrayType.construct(
                    getJacksonType(arrType.getGenericComponentType()),
                    null
            );
        }
        Class<?> clazz = (Class<?>) type;
        if (clazz.isArray()) {
            return ArrayType.construct(
                    getJacksonType(clazz.getComponentType()),
                    null
            );
        }
        return SimpleType.constructUnsafe(clazz);
    }
}
