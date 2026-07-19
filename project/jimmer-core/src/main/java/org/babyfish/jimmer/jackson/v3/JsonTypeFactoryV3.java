package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.jackson.codec.JsonTypeFactory;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class JsonTypeFactoryV3 implements JsonTypeFactory<JavaType> {
    private final TypeFactory typeFactory;

    public JsonTypeFactoryV3(TypeFactory typeFactory) {
        this.typeFactory = typeFactory;
    }

    @Override
    public JavaType constructType(Type type) {
        return typeFactory.constructType(type);
    }

    @Override
    public JavaType constructParametricType(Class<?> parametrized, Class<?>... parameterClasses) {
        return typeFactory.constructParametricType(parametrized, parameterClasses);
    }

    @Override
    public JavaType constructParametricType(Class<?> parametrized, JavaType... parameterClasses) {
        return typeFactory.constructParametricType(parametrized, parameterClasses);
    }

    @Override
    public JavaType constructArrayType(Class<?> componentType) {
        return typeFactory.constructArrayType(componentType);
    }

    @Override
    public JavaType constructArrayType(JavaType componentType) {
        return typeFactory.constructArrayType(componentType);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public JavaType constructCollectionType(Class<? extends Collection> collectionType, Class<?> elementType) {
        return typeFactory.constructCollectionType(collectionType, elementType);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public JavaType constructCollectionType(Class<? extends Collection> collectionType, JavaType elementType) {
        return typeFactory.constructCollectionType(collectionType, elementType);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public JavaType constructMapType(Class<? extends Map> mapType, Class<?> keyType, Class<?> valueType) {
        return typeFactory.constructMapType(mapType, keyType, valueType);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public JavaType constructMapType(Class<? extends Map> mapType, JavaType keyType, JavaType valueType) {
        return typeFactory.constructMapType(mapType, keyType, valueType);
    }
}
