package org.babyfish.jimmer.sql.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.Internal;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class ValueSerializer<T> {

    private static final byte[] NULL_BYTES = "<null>".getBytes(StandardCharsets.UTF_8);

    private final ObjectMapper mapper;

    private final JavaType valueType;

    private final boolean requireNewDraftContext;

    public ValueSerializer(@NotNull ImmutableType type) {
        this(type, null, null);
    }

    public ValueSerializer(@NotNull ImmutableProp prop) {
        this(null, prop, null);
    }

    public ValueSerializer(@NotNull ImmutableType type, ObjectMapper mapper) {
        this(type, null, mapper);
    }

    public ValueSerializer(@NotNull ImmutableProp prop, ObjectMapper mapper) {
        this(null, prop, mapper);
    }

    private ValueSerializer(ImmutableType type, ImmutableProp prop, ObjectMapper mapper) {
        if ((type == null) == (prop == null)) {
            throw new IllegalArgumentException("Internal bug: nullity of type and prop must be different");
        }
        ObjectMapper clonedMapper = mapper != null?
            new ObjectMapper(mapper) {} :
            new ObjectMapper();
        clonedMapper.registerModule(new ImmutableModule());
        this.mapper = clonedMapper;
        if (prop == null) {
            this.valueType = SimpleType.constructUnsafe(type.getJavaClass());
        } else if (prop.isAssociation(TargetLevel.OBJECT)) {
            ImmutableProp targetIdProp = prop.getTargetType().getIdProp();
            if (targetIdProp == null) {
                throw new IllegalArgumentException("Transient association property is not supported");
            }
            JavaType targetIdType = SimpleType.constructUnsafe(
                    targetIdProp.getElementClass()
            );
            if (prop.isReferenceList(TargetLevel.ENTITY)) {
                this.valueType = CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        targetIdType
                );
            } else {
                this.valueType = targetIdType;
            }
        } else {
            this.valueType = SimpleType.constructUnsafe(prop.getElementClass());
        }
        this.requireNewDraftContext = type != null;
    }

    @NotNull
    public byte[] serialize(T value) {
        if (value == null) {
            return NULL_BYTES.clone();
        }
        try {
            return mapper.writeValueAsBytes(value);
        } catch (JsonProcessingException ex) {
            throw new ValueSerializationException(ex);
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
        if (!requireNewDraftContext) {
            return deserializeImpl(value, null);
        }
        return Internal.requiresNewDraftContext(ctx -> deserializeImpl(value, ctx));
    }

    @NotNull
    public <K> Map<K, T> deserialize(@NotNull Map<K, byte[]> map) {
        Map<K, T> deserializedMap = new LinkedHashMap<>((map.size() * 4 + 2) / 3);
        if (!requireNewDraftContext) {
            for (Map.Entry<K, byte[]> e : map.entrySet()) {
                deserializedMap.put(e.getKey(), deserializeImpl(e.getValue(), null));
            }
        } else {
            Internal.requiresNewDraftContext(ctx -> {
                for (Map.Entry<K, byte[]> e : map.entrySet()) {
                    deserializedMap.put(e.getKey(), deserializeImpl(e.getValue(), ctx));
                }
                return null;
            });
        }
        return deserializedMap;
    }

    @NotNull
    public <K1, K2> Map<K2, T> deserialize(@NotNull Map<K1, byte[]> map, @NotNull Function<K1, K2> keyMapper) {
        Map<K2, T> deserializedMap = new LinkedHashMap<>((map.size() * 4 + 2) / 3);
        if (!requireNewDraftContext) {
            for (Map.Entry<K1, byte[]> e : map.entrySet()) {
                deserializedMap.put(keyMapper.apply(e.getKey()), deserializeImpl(e.getValue(), null));
            }
        } else {
            Internal.requiresNewDraftContext(ctx -> {
                for (Map.Entry<K1, byte[]> e : map.entrySet()) {
                    deserializedMap.put(keyMapper.apply(e.getKey()), deserializeImpl(e.getValue(), ctx));
                }
                return null;
            });
        }
        return deserializedMap;
    }

    @NotNull
    public <K> Map<K, T> deserialize(@NotNull Collection<K> keys, @NotNull Collection<byte[]> values) {
        Map<K, T> deserializedMap = new LinkedHashMap<>((keys.size() * 4 + 2) / 3);
        if (!requireNewDraftContext) {
            Iterator<K> keyItr = keys.iterator();
            Iterator<byte[]> byteArrItr = values.iterator();
            while (keyItr.hasNext() && byteArrItr.hasNext()) {
                K key = keyItr.next();
                byte[] byteArr = byteArrItr.next();
                if (byteArr != null) {
                    deserializedMap.put(key, deserializeImpl(byteArr, null));
                }
            }
        } else {
            Internal.requiresNewDraftContext(ctx -> {
                Iterator<K> keyItr = keys.iterator();
                Iterator<byte[]> byteArrItr = values.iterator();
                while (keyItr.hasNext() && byteArrItr.hasNext()) {
                    K key = keyItr.next();
                    byte[] byteArr = byteArrItr.next();
                    if (byteArr != null) {
                        deserializedMap.put(key, deserializeImpl(byteArr, ctx));
                    }
                }
                return null;
            });
        }
        return deserializedMap;
    }

    private T deserializeImpl(byte[] value, DraftContext ctx) {
        if (value == null || value.length == 0 || Arrays.equals(value, NULL_BYTES)) {
            return null;
        }
        T deserializedValue;
        try {
            deserializedValue = mapper.readValue(value, valueType);
        } catch (IOException ex) {
            throw new ValueSerializationException(ex);
        }
        return ctx != null ? ctx.resolveObject(deserializedValue) : deserializedValue;
    }
}
