package org.babyfish.jimmer.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A declared Java type, including type arguments when present. */
public final class JsonType<T> {

    private final Type type;

    private JsonType(Type type) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    public static <T> JsonType<T> of(Class<T> type) {
        return new JsonType<>(type);
    }

    public static JsonType<?> of(Type type) {
        return new JsonType<>(type);
    }

    public static <T> JsonType<List<T>> listOf(JsonType<T> elementType) {
        return new JsonType<>(JsonTypeFactory.INSTANCE.constructListType(elementType.type));
    }

    public static <K, V> JsonType<Map<K, V>> mapOf(JsonType<K> keyType, JsonType<V> valueType) {
        return new JsonType<>(JsonTypeFactory.INSTANCE.constructMapType(keyType.type, valueType.type));
    }

    public Type getType() {
        return type;
    }
}
