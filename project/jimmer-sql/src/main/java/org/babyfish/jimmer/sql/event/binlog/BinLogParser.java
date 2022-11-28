package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;

public class BinLogParser {

    private ObjectMapper mapper;

    public BinLogParser initialize(JSqlClient sqlClient) {
        return initialize(sqlClient, null);
    }

    public BinLogParser initialize(JSqlClient sqlClient, ObjectMapper mapper) {
        if (sqlClient == null) {
            throw new IllegalArgumentException("`sqlClient` cannot be null");
        }
        ObjectMapper clonedMapper = mapper != null ?
                new ObjectMapper(mapper) {} :
                new ObjectMapper();
        clonedMapper
                .registerModule(new BinLogModule(sqlClient))
                .registerModule(new JavaTimeModule());
        this.mapper = clonedMapper;
        return this;
    }

    public <T> T parseEntity(@NotNull Class<T> type, String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Illegal json: " + json, ex);
        }
    }

    public <T> T parseEntity(@NotNull Class<T> type, JsonNode data) {
        if (data == null || data.isNull()) {
            return null;
        }
        return parseEntity(type, data.toString());
    }

    @SuppressWarnings("unchecked")
    public <T> T parseEntity(@NotNull ImmutableType type, String json) {
        if (type instanceof AssociationType) {
            throw new IllegalArgumentException("type cannot be AssociationType");
        }
        return (T)parseEntity(type.getJavaClass(), json);
    }

    public <T> T parseEntity(@NotNull ImmutableType type, JsonNode data) {
        if (data == null || data.isNull()) {
            return null;
        }
        return parseEntity(type, data.toString());
    }

    @SuppressWarnings("unchecked")
    public <S, T> Tuple2<S, T> parseIdPair(@NotNull AssociationType associationType, JsonNode data) {
        Iterator<Map.Entry<String, JsonNode>> itr = data.fields();
        Object sourceId = null;
        Object targetId = null;
        while (itr.hasNext()) {
            Map.Entry<String, JsonNode> e = itr.next();
            ImmutableProp targetProp = associationType.getPropByColumnName(e.getKey());
            JsonNode childNode = e.getValue();
            Class<?> targetIdType = targetProp.getTargetType().getIdProp().getElementClass();
            Object value;
            if (targetIdType == byte.class || targetIdType == Byte.class) {
                value = (byte)childNode.asInt();
            } else if (targetIdType == short.class || targetIdType == Short.class) {
                value = (short)childNode.asInt();
            } else if (targetIdType == int.class || targetIdType == Integer.class) {
                value = childNode.asInt();
            } else if (targetIdType == long.class || targetIdType == Long.class) {
                value = childNode.asLong();
            } else if (targetIdType == String.class) {
                value = childNode.asText();
            } else {
                String content = childNode.toString();
                try {
                    value = mapper.readValue(content, targetIdType);
                } catch (JsonProcessingException ex) {
                    throw new IllegalArgumentException(
                            "Cannot map \"" +
                                    content +
                                    "\" to target id type of \"" +
                                    targetProp +
                                    "\""
                    );
                }
            }
            if (targetProp == associationType.getSourceProp()) {
                sourceId = value;
            } else {
                targetId = value;
            }
        }
        if (sourceId == null || targetId == null) {
            throw new IllegalArgumentException("missing some fields of middle table : " + data);
        }
        return new Tuple2<>((S)sourceId, (T)targetId);
    }

    public <S, T> Tuple2<S, T> parseIdPair(@NotNull AssociationType associationType, String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        JsonNode data;
        try {
            data = mapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Illegal json: " + json, ex);
        }
        return parseIdPair(associationType, data);
    }

    public <S, T> Tuple2<S, T> parseIdPair(@NotNull TypedProp<?, ?> prop, JsonNode data) {
        return parseIdPair(AssociationType.of(prop.unwrap()), data);
    }

    public <S, T> Tuple2<S, T> parseIdPair(@NotNull ImmutableProp prop, JsonNode data) {
        return parseIdPair(AssociationType.of(prop), data);
    }

    public <S, T> Tuple2<S, T> parseIdPair(@NotNull TypedProp<?, ?> prop, String json) {
        return parseIdPair(AssociationType.of(prop.unwrap()), json);
    }

    public <S, T> Tuple2<S, T> parseIdPair(@NotNull ImmutableProp prop, String json) {
        return parseIdPair(AssociationType.of(prop), json);
    }
}
