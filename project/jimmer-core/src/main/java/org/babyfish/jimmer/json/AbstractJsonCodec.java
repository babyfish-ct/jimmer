package org.babyfish.jimmer.json;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Optional base for engines with string-based read/write entry points and ordinary Java value trees. */
public abstract class AbstractJsonCodec implements JsonCodec {

    protected abstract Object read(String json, Type type);

    /** A null type requests the runtime type. */
    protected abstract String write(Object value, Type type, boolean pretty);

    @Override
    public <T> JsonReader<T> readerFor(Type type) {
        return new JsonReader<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public T read(String json) {
                return (T) AbstractJsonCodec.this.read(json, type);
            }

            @Override
            public T read(byte[] json) {
                return read(new String(json, StandardCharsets.UTF_8));
            }

            @Override
            public T read(Reader reader) throws IOException {
                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[4096];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    builder.append(buffer, 0, count);
                }
                return read(builder.toString());
            }

            @Override
            public T read(InputStream stream) throws IOException {
                return read(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        };
    }

    @Override
    public JsonWriter<Object> writer() {
        return new WriterImpl<>(null, false);
    }

    @Override
    public <T> JsonWriter<T> writerFor(Type type) {
        return new WriterImpl<>(type, false);
    }

    private class WriterImpl<T> implements JsonWriter<T> {

        private final Type type;
        private final boolean pretty;

        WriterImpl(Type type, boolean pretty) {
            this.type = type;
            this.pretty = pretty;
        }

        @Override
        public JsonWriter<T> withDefaultPrettyPrinter() {
            return pretty ? this : new WriterImpl<>(type, true);
        }

        @Override
        public String writeAsString(T value) {
            return AbstractJsonCodec.this.write(value, type, pretty);
        }

        @Override
        public byte[] writeAsBytes(T value) {
            return writeAsString(value).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void write(Writer writer, T value) throws IOException {
            writer.write(writeAsString(value));
        }

        @Override
        public void write(OutputStream stream, T value) throws IOException {
            stream.write(writeAsBytes(value));
        }
    }

    @Override
    public JsonConverter converter() {
        return new JsonConverter() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T convert(Object value, Type type) {
                return (T) read(write(value, null, false), type);
            }
        };
    }

    @Override
    public JsonReader<Node> treeReader() {
        return new MappingJsonReader<>(readerFor(Object.class), ValueNode::new);
    }

    private class ValueNode implements Node {

        private final Object value;

        ValueNode(Object value) {
            this.value = value;
        }

        @Override
        public Node get(int index) {
            if (!(value instanceof List<?>) || index < 0 || index >= ((List<?>) value).size()) {
                return null;
            }
            return new ValueNode(((List<?>) value).get(index));
        }

        @Override
        public Node get(String name) {
            if (!(value instanceof Map<?, ?>) || !((Map<?, ?>) value).containsKey(name)) {
                return null;
            }
            return new ValueNode(((Map<?, ?>) value).get(name));
        }

        @Override
        @SuppressWarnings("unchecked")
        public Iterator<Map.Entry<String, Node>> fieldsIterator() {
            if (!(value instanceof Map<?, ?>)) {
                return Collections.emptyIterator();
            }
            return ((Map<String, ?>) value).entrySet().stream().<Map.Entry<String, Node>>map(e ->
                    new AbstractMap.SimpleImmutableEntry<>(e.getKey(), new ValueNode(e.getValue()))
            ).iterator();
        }

        @Override
        public Kind kind() {
            if (value == null) return Kind.NULL;
            if (value instanceof List<?>) return Kind.ARRAY;
            if (value instanceof Map<?, ?>) return Kind.OBJECT;
            if (value instanceof Number) return Kind.NUMBER;
            if (value instanceof Boolean) return Kind.BOOLEAN;
            return Kind.STRING;
        }

        @Override
        public int size() {
            if (value instanceof Map<?, ?>) {
                return ((Map<?, ?>) value).size();
            }
            return value instanceof List<?> ? ((List<?>) value).size() : 0;
        }

        @Override
        public Object toJavaValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Node && contentEquals((Node) o);
        }

        @Override
        public int hashCode() {
            return contentHashCode();
        }

        @Override
        public String toString() {
            return write(value, null, false);
        }
    }
}
