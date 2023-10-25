package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.meta.TargetLevel;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PropUtils {

    public static JavaType getJacksonType(ImmutableProp prop) {
        if (prop.isReferenceList(TargetLevel.OBJECT) || prop.isScalarList()) {
            return CollectionType.construct(
                    List.class,
                    null,
                    null,
                    null,
                    SimpleType.constructUnsafe(prop.getElementClass())
            );
        }
        try {
            return jacksonType(prop.getGenericType());
        } catch (RuntimeException ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            "prop" +
                            "\", cannot create jackson property: " +
                            ex.getMessage(),
                    ex
            );
        }
    }

    private static JavaType jacksonType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType == Collection.class || rawType == List.class || rawType == Set.class) {
                return CollectionType.construct(
                        (Class<?>) rawType,
                        null,
                        null,
                        null,
                        jacksonType(parameterizedType.getActualTypeArguments()[0])
                );
            }
            if (rawType == Map.class) {
                return MapType.construct(
                        (Class<?>) rawType,
                        null,
                        null,
                        null,
                        jacksonType(parameterizedType.getActualTypeArguments()[0]),
                        jacksonType(parameterizedType.getActualTypeArguments()[1])
                );
            }
            throw new IllegalArgumentException("Parameterized type must be collection, list, set or map");
        }
        if (type instanceof TypeVariable<?>) {
            return jacksonType(((TypeVariable<?>)type).getBounds()[0]);
        }
        if (type instanceof WildcardType) {
            return jacksonType(((WildcardType)type).getUpperBounds()[0]);
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType arrType = (GenericArrayType) type;
            return ArrayType.construct(
                    jacksonType(arrType.getGenericComponentType()),
                    null
            );
        }
        Class<?> clazz = (Class<?>) type;
        if (clazz.isArray()) {
            return ArrayType.construct(
                    jacksonType(clazz.getComponentType()),
                    null
            );
        }
        return SimpleType.constructUnsafe(clazz);
    }
}
