package org.babyfish.jimmer.jackson.codec;

import java.util.List;
import java.util.Map;

/**
 * Facade to abstract jimmer from concrete JSON implementation.
 * Allows to support jackson 2, jackson 3 and other JSON codecs simultaneously.
 *
 * @param <JT> implementation-specific type token
 */
public interface JsonCodec<JT> {
    static JsonCodec<?> jsonCodec() {
        return JsonCodecDetector.JSON_CODEC;
    }

    JsonCodec<JT> withCustomizations(JsonCodecCustomization... customizations);

    JsonConverter converter();

    <T> JsonReader<T> readerFor(Class<T> clazz);

    <T> JsonReader<T> readerFor(TypeCreator<JT> typeCreator);

    <T> JsonReader<T[]> readerForArrayOf(Class<T> componentType);

    <T> JsonReader<List<T>> readerForListOf(Class<T> elementType);

    <V> JsonReader<Map<String, V>> readerForMapOf(Class<V> valueType);

    default <K, V> JsonReader<Map<K, V>> readerForMapOf(Class<K> keyType, Class<V> valueType) {
        return readerFor(tf -> tf.constructMapType(keyType, valueType));
    }

    JsonReader<Node> treeReader();

    JsonWriter writer();

    JsonWriter writerFor(Class<?> clazz);

    JsonWriter writerFor(TypeCreator<JT> typeCreator);

    JacksonVersion version();
}
