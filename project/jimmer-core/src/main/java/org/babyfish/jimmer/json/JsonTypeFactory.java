package org.babyfish.jimmer.json;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.lang.Generics;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class JsonTypeFactory {

    public static final JsonTypeFactory INSTANCE = new JsonTypeFactory();

    private JsonTypeFactory() {}

    public Type constructType(Type type) {
        return type;
    }

    public Type constructParametricType(Class<?> rawType, Type... arguments) {
        Type[] types = arguments.clone();
        for (int i = 0; i < types.length; i++) {
            if (types[i] instanceof Class<?>) {
                types[i] = Classes.boxTypeOf((Class<?>) types[i]);
            }
        }
        return Generics.makeParameterizedType(rawType, types);
    }

    public Type constructArrayType(Type elementType) {
        return elementType instanceof Class<?> ?
                Array.newInstance((Class<?>) elementType, 0).getClass() :
                Generics.makeGenericArrayType(elementType);
    }

    public Type constructCollectionType(Class<? extends Collection> collectionType, Type elementType) {
        return constructParametricType(collectionType, elementType);
    }

    public Type constructListType(Type elementType) {
        return constructCollectionType(List.class, elementType);
    }

    public Type constructMapType(Class<? extends Map> mapType, Type keyType, Type valueType) {
        return constructParametricType(mapType, keyType, valueType);
    }

    public Type constructMapType(Type keyType, Type valueType) {
        return constructMapType(Map.class, keyType, valueType);
    }
}
