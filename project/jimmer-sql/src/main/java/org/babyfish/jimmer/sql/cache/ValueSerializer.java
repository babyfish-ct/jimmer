package org.babyfish.jimmer.sql.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class ValueSerializer<T> {

    private static final byte[] NULL_BYTES = "<null>".getBytes(StandardCharsets.UTF_8);

    private final ObjectMapper mapper;

    private JavaType valueType;

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
        if (prop != null) {
            JavaType targetIdType = SimpleType.constructUnsafe(
                    prop.getTargetType().getIdProp().getElementClass()
            );
            if (prop.isEntityList()) {
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
            this.valueType = SimpleType.constructUnsafe(type.getJavaClass());
        }
    }

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

    public T deserialize(byte[] value) {
        if (value == null || value.length == 0 || Arrays.equals(value, NULL_BYTES)) {
            return null;
        }
        try {
            return mapper.readValue(value, valueType);
        } catch (IOException ex) {
            throw new ValueSerializationException(ex);
        }
    }
}
