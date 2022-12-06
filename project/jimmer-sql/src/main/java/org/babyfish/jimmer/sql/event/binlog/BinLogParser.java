package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.association.meta.AssociationProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.util.EmbeddableObjects;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BinLogParser {

    private ObjectMapper mapper;

    private JSqlClient sqlClient;

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
        this.sqlClient = sqlClient;
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

        AssociationProp sourceProp = associationType.getSourceProp();
        AssociationProp targetProp = associationType.getTargetProp();
        ImmutableProp sourceIdProp = sourceProp.getTargetType().getIdProp();
        ImmutableProp targetIdProp = targetProp.getTargetType().getIdProp();
        Object sourceId = null;
        Object targetId = null;

        if (sourceIdProp.isEmbedded(EmbeddedLevel.SCALAR)) {
            sourceId = Internal.produce(sourceIdProp.getTargetType(), null, draft -> {
                Iterator<Map.Entry<String, JsonNode>> itr = data.fields();
                while (itr.hasNext()) {
                    Map.Entry<String, JsonNode> e = itr.next();
                    List<ImmutableProp> chain = associationType.getPropChainByColumnName(e.getKey());
                    if (chain.get(0) == sourceProp) {
                        ValueParser.addEntityProp(
                                (DraftSpi) draft,
                                chain.subList(2, chain.size()),
                                e.getValue(),
                                sqlClient
                        );
                    }
                }
            });
            if (sourceId == null || !EmbeddableObjects.isCompleted(sourceId)) {
                throw new IllegalArgumentException(
                        "source id fields of \"" +
                                associationType +
                                "\" cannot be null"
                );
            }
        }

        if (targetIdProp.isEmbedded(EmbeddedLevel.SCALAR)) {
            targetId = Internal.produce(targetIdProp.getTargetType(), null, draft -> {
                Iterator<Map.Entry<String, JsonNode>> itr = data.fields();
                while (itr.hasNext()) {
                    Map.Entry<String, JsonNode> e = itr.next();
                    List<ImmutableProp> chain = associationType.getPropChainByColumnName(e.getKey());
                    if (chain.get(0) == targetProp) {
                        ValueParser.addEntityProp(
                                (DraftSpi) draft,
                                chain.subList(2, chain.size()),
                                e.getValue(),
                                sqlClient
                        );
                    }
                }
            });
            if (targetId == null || !EmbeddableObjects.isCompleted(targetId)) {
                throw new IllegalArgumentException(
                        "target id fields of \"" +
                                associationType +
                                "\" cannot be null"
                );
            }
        }

        if (sourceId == null || targetId == null) {
            Iterator<Map.Entry<String, JsonNode>> itr = data.fields();
            while (itr.hasNext()) {
                Map.Entry<String, JsonNode> e = itr.next();
                List<ImmutableProp> chain = associationType.getPropChainByColumnName(e.getKey());
                ImmutableProp prop = chain.get(0);
                if (prop == sourceProp) {
                    sourceId = ValueParser.parseSingleValue(
                            sqlClient,
                            e.getValue(),
                            sourceIdProp.getElementClass(),
                            false
                    );
                    if (sourceId == null) {
                        throw new IllegalArgumentException(
                                "source id fields of \"" +
                                        associationType +
                                        "\" cannot be null"
                        );
                    }
                } else if (prop == targetProp) {
                    targetId = ValueParser.parseSingleValue(
                            sqlClient,
                            e.getValue(),
                            targetIdProp.getElementClass(),
                            false
                    );
                    if (targetId == null) {
                        throw new IllegalArgumentException(
                                "target id fields of \"" +
                                        associationType +
                                        "\" cannot be null"
                        );
                    }
                }
            }
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
