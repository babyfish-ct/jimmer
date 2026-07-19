package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.jackson.codec.JsonReader;
import org.babyfish.jimmer.jackson.codec.JsonTypeFactory;
import org.babyfish.jimmer.jackson.codec.JsonWriter;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.exception.SerializationException;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;

public class ValueSerializer<T> {
    private static final byte[] NULL_BYTES = "<null>".getBytes(StandardCharsets.UTF_8);

    private final JsonReader<T> jsonReader;
    private final JsonWriter jsonWriter;

    public ValueSerializer(@NotNull ImmutableType type) {
        this(type, null, jsonCodec());
    }

    public ValueSerializer(@NotNull ImmutableProp prop) {
        this(null, prop, jsonCodec());
    }

    public ValueSerializer(@NotNull ImmutableType type, @NotNull JsonCodec<?> codec) {
        this(type, null, codec);
    }

    public ValueSerializer(@NotNull ImmutableProp prop, @NotNull JsonCodec<?> codec) {
        this(null, prop, codec);
    }

    private ValueSerializer(ImmutableType type, ImmutableProp prop, JsonCodec<?> codec) {
        if ((type == null) == (prop == null)) {
            throw new IllegalArgumentException("Internal bug: nullity of type and prop must be different");
        }
        this.jsonReader = codec.readerFor(tf -> createValueType(tf, type, prop));
        this.jsonWriter = codec.writer();
    }

    private static <JT> JT createValueType(JsonTypeFactory<JT> typeFactory, ImmutableType type, ImmutableProp prop) {
        if (prop == null) {
            return typeFactory.constructType(type.getJavaClass());
        } else if (prop.isAssociation(TargetLevel.ENTITY)) {
            ImmutableProp targetIdProp = prop.getTargetType().getIdProp();
            if (prop.isReferenceList(TargetLevel.OBJECT)) {
                return typeFactory.constructListType(targetIdProp.getElementClass());
            } else {
                return typeFactory.constructType(targetIdProp.getElementClass());
            }
        } else {
            return typeFactory.constructType(prop.getElementClass());
        }
    }

    @NotNull
    public byte[] serialize(T value) {
        if (value == null) {
            return NULL_BYTES.clone();
        }
        try {
            return jsonWriter.writeAsBytes(value);
        } catch (Exception ex) {
            throw new SerializationException(ex);
        }
    }

    @NotNull
    public <K> Map<K, byte[]> serialize(@NotNull Map<K, T> map) {
        Map<K, byte[]> serializedMap = new LinkedHashMap<>((map.size() * 4 + 2) / 3);
        for (Map.Entry<K, T> e : map.entrySet()) {
            serializedMap.put(e.getKey(), serialize(e.getValue()));
        }
        return serializedMap;
    }

    @NotNull
    public <K1, K2> Map<K2, byte[]> serialize(@NotNull Map<K1, T> map, @NotNull Function<K1, K2> keyMapper) {
        Map<K2, byte[]> serializedMap = new LinkedHashMap<>((map.size() * 4 + 2) / 3);
        for (Map.Entry<K1, T> e : map.entrySet()) {
            serializedMap.put(keyMapper.apply(e.getKey()), serialize(e.getValue()));
        }
        return serializedMap;
    }

    public T deserialize(byte[] value) {
        if (value == null || value.length == 0 || Arrays.equals(value, NULL_BYTES)) {
            return null;
        }
        try {
            return jsonReader.read(value);
        } catch (Exception ex) {
            throw new SerializationException(ex);
        }
    }

    @NotNull
    public <K> Map<K, T> deserialize(@NotNull Map<K, byte[]> map) {
        Map<K, T> deserializedMap = new LinkedHashMap<>((map.size() * 4 + 2) / 3);
        for (Map.Entry<K, byte[]> e : map.entrySet()) {
            deserializedMap.put(e.getKey(), deserialize(e.getValue()));
        }
        return deserializedMap;
    }

    @NotNull
    public <K1, K2> Map<K2, T> deserialize(@NotNull Map<K1, byte[]> map, @NotNull Function<K1, K2> keyMapper) {
        Map<K2, T> deserializedMap = new LinkedHashMap<>((map.size() * 4 + 2) / 3);
        for (Map.Entry<K1, byte[]> e : map.entrySet()) {
            deserializedMap.put(keyMapper.apply(e.getKey()), deserialize(e.getValue()));
        }
        return deserializedMap;
    }

    @NotNull
    public <K> Map<K, T> deserialize(@NotNull Collection<K> keys, @NotNull Collection<byte[]> values) {
        Map<K, T> deserializedMap = new LinkedHashMap<>((keys.size() * 4 + 2) / 3);
        Iterator<K> keyItr = keys.iterator();
        Iterator<byte[]> byteArrItr = values.iterator();
        while (keyItr.hasNext() && byteArrItr.hasNext()) {
            K key = keyItr.next();
            byte[] byteArr = byteArrItr.next();
            if (byteArr != null) {
                deserializedMap.put(key, deserialize(byteArr));
            }
        }
        return deserializedMap;
    }
}
