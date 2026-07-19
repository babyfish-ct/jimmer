package org.babyfish.jimmer.jackson.codec;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface JsonTypeFactory<JT> {
    JT constructType(Type type);

    JT constructParametricType(Class<?> parametrized, Class<?>... parameterClasses);

    JT constructParametricType(Class<?> parametrized, JT[] parameterClasses);

    JT constructArrayType(Class<?> componentType);

    JT constructArrayType(JT componentType);

    @SuppressWarnings("rawtypes")
    JT constructCollectionType(Class<? extends Collection> collectionType, Class<?> elementType);

    @SuppressWarnings("rawtypes")
    JT constructCollectionType(Class<? extends Collection> collectionType, JT elementType);

    @SuppressWarnings("rawtypes")
    JT constructMapType(Class<? extends Map> mapType, Class<?> keyType, Class<?> valueType);

    @SuppressWarnings("rawtypes")
    JT constructMapType(Class<? extends Map> mapType, JT keyType, JT valueType);

    default JT constructListType(Class<?> elementType) {
        return constructCollectionType(List.class, elementType);
    }

    default JT constructMapType(Class<?> keyType, Class<?> valueType) {
        return constructMapType(Map.class, keyType, valueType);
    }
}
