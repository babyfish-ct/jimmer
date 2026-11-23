package org.babyfish.jimmer.json;

import java.io.Writer;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/** JSON operations associated with one configured mapper. */
public interface JsonCodec {

    static JsonCodec jsonCodec() {
        return JsonCodecs.defaultCodec();
    }

    default <T> JsonReader<T> reader(JsonType<T> type) {
        return readerFor(type.getType());
    }

    default <T> JsonWriter<T> writer(JsonType<T> type) {
        return writerFor(type.getType());
    }

    <T> JsonReader<T> readerFor(Type type);

    default <T> JsonReader<T> readerFor(Class<T> type) {
        return readerFor((Type) type);
    }

    default <T> JsonReader<T> readerFor(TypeCreator creator) {
        return readerFor(creator.createType(JsonTypeFactory.INSTANCE));
    }

    default <T> JsonReader<T[]> readerForArrayOf(Class<T> type) {
        return readerFor(tf -> tf.constructArrayType(type));
    }

    default <T> JsonReader<List<T>> readerForListOf(Class<T> type) {
        return readerFor(tf -> tf.constructListType(type));
    }

    default <V> JsonReader<Map<String, V>> readerForMapOf(Class<V> type) {
        return readerForMapOf(String.class, type);
    }

    default <K, V> JsonReader<Map<K, V>> readerForMapOf(Class<K> key, Class<V> value) {
        return readerFor(tf -> tf.constructMapType(key, value));
    }

    JsonWriter<Object> writer();

    <T> JsonWriter<T> writerFor(Type type);

    default <T> JsonWriter<T> writerFor(TypeCreator creator) {
        return writerFor(creator.createType(JsonTypeFactory.INSTANCE));
    }

    JsonReader<Node> treeReader();

    /**
     * Writes a JSON tree composed of string-keyed maps, lists, scalars and null.
     * Keys and explicit nulls are part of the document. Implementations with configurable
     * object mapping must bypass naming, inclusion and polymorphic type settings here.
     * The caller owns the writer.
     */
    default void writeTree(Writer target, Object tree) throws Exception {
        target.write(writer().withDefaultPrettyPrinter().writeAsString(tree));
    }

    JsonConverter converter();

    default <T> T fromTree(Node node, JsonType<T> type) throws Exception {
        return converter().convert(node.toJavaValue(), type.getType());
    }

    default <T> Node toTree(T value, JsonType<T> type) throws Exception {
        return treeReader().read(writer(type).writeAsBytes(value));
    }
}
