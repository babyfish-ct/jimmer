package org.babyfish.jimmer.json.codec;

import org.babyfish.jimmer.lang.Generics;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JsonType {

    private final Type type;

    private JsonType(Type type) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    @NotNull
    public static JsonType of(@NotNull Type type) {
        return new JsonType(type);
    }

    @NotNull
    public static JsonType of(@NotNull Class<?> type) {
        return new JsonType(type);
    }

    @NotNull
    public static JsonType parameterized(@NotNull Class<?> rawType, @NotNull Class<?>... argumentTypes) {
        Type[] types = new Type[argumentTypes.length];
        System.arraycopy(argumentTypes, 0, types, 0, argumentTypes.length);
        return new JsonType(Generics.makeParameterizedType(rawType, types));
    }

    @NotNull
    public static JsonType parameterized(@NotNull Class<?> rawType, @NotNull JsonType... argumentTypes) {
        Type[] types = new Type[argumentTypes.length];
        for (int i = argumentTypes.length - 1; i >= 0; --i) {
            types[i] = argumentTypes[i].type;
        }
        return new JsonType(Generics.makeParameterizedType(rawType, types));
    }

    @NotNull
    public static JsonType arrayOf(@NotNull Class<?> componentType) {
        return new JsonType(Array.newInstance(componentType, 0).getClass());
    }

    @NotNull
    public static JsonType arrayOf(@NotNull JsonType componentType) {
        Type type = componentType.type;
        if (type instanceof Class<?>) {
            return arrayOf((Class<?>) type);
        }
        return new JsonType(Generics.makeGenericArrayType(type));
    }

    @NotNull
    public static JsonType collectionOf(
            @NotNull Class<? extends Collection> collectionType,
            @NotNull Class<?> elementType
    ) {
        return collectionOf(collectionType, JsonType.of(elementType));
    }

    @NotNull
    public static JsonType collectionOf(
            @NotNull Class<? extends Collection> collectionType,
            @NotNull JsonType elementType
    ) {
        return parameterized(collectionType, elementType);
    }

    @NotNull
    public static JsonType listOf(@NotNull Class<?> elementType) {
        return collectionOf(List.class, elementType);
    }

    @NotNull
    public static JsonType listOf(@NotNull JsonType elementType) {
        return collectionOf(List.class, elementType);
    }

    @NotNull
    public static JsonType mapOf(@NotNull Class<?> keyType, @NotNull Class<?> valueType) {
        return mapOf(Map.class, keyType, valueType);
    }

    @NotNull
    public static JsonType mapOf(@NotNull Class<? extends Map> mapType, @NotNull Class<?> keyType, @NotNull Class<?> valueType) {
        return mapOf(mapType, JsonType.of(keyType), JsonType.of(valueType));
    }

    @NotNull
    public static JsonType mapOf(@NotNull Class<? extends Map> mapType, @NotNull JsonType keyType, @NotNull JsonType valueType) {
        return parameterized(mapType, keyType, valueType);
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.getTypeName();
    }
}
