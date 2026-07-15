package org.babyfish.jimmer.json.codec;

import java.util.List;
import java.util.Map;

/**
 * 抽象 Jimmer 与具体 JSON 实现的门面。
 */
public interface JsonCodec {
    static JsonCodec jsonCodec() {
        return JsonCodecDetector.JSON_CODEC;
    }

    JsonCodec withCustomizations(JsonCodecCustomization... customizations);

    JsonConverter converter();

    default <T> JsonReader<T> readerFor(Class<T> clazz) {
        return readerFor(JsonType.of(clazz));
    }

    <T> JsonReader<T> readerFor(JsonType type);

    default <T> JsonReader<T[]> readerForArrayOf(Class<T> componentType) {
        return readerFor(JsonType.arrayOf(componentType));
    }

    default <T> JsonReader<List<T>> readerForListOf(Class<T> elementType) {
        return readerFor(JsonType.listOf(elementType));
    }

    default <V> JsonReader<Map<String, V>> readerForMapOf(Class<V> valueType) {
        return readerFor(JsonType.mapOf(String.class, valueType));
    }

    default <K, V> JsonReader<Map<K, V>> readerForMapOf(Class<K> keyType, Class<V> valueType) {
        return readerFor(JsonType.mapOf(keyType, valueType));
    }

    JsonReader<Node> treeReader();

    JsonWriter writer();

    default JsonWriter writerFor(Class<?> clazz) {
        return writerFor(JsonType.of(clazz));
    }

    JsonWriter writerFor(JsonType type);
}
