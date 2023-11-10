package org.babyfish.jimmer.sql.event.binlog.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.impl.util.PropCache;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.association.meta.AssociationProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.util.EmbeddableObjects;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.binlog.BinLogPropReader;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BinLogParser {

    private final Map<String, BinLogPropReader> readerMap = new HashMap<>();

    private final Map<Class<?>, BinLogPropReader> typeReaderMap = new HashMap<>();

    private final PropCache<BinLogPropReader> readerCache = new PropCache<>(this::createReader, true);

    private ObjectMapper mapper;

    private JSqlClientImplementor sqlClient;

    public BinLogParser initialize(
            JSqlClientImplementor sqlClient,
            ObjectMapper mapper,
            Map<ImmutableProp, BinLogPropReader> propReaderMap,
            Map<Class<?>, BinLogPropReader> typePropReaderMap
    ) {
        if (sqlClient == null) {
            throw new IllegalArgumentException("`sqlClient` cannot be null");
        }
        ObjectMapper clonedMapper = mapper != null ?
                new ObjectMapper(mapper) {} :
                new ObjectMapper();
        clonedMapper
                .registerModule(new BinLogModule(this))
                .registerModule(new JavaTimeModule());
        this.mapper = clonedMapper;
        this.sqlClient = sqlClient;
        Map<String, BinLogPropReader> propNameReaderMap = new HashMap<>();
        for (ImmutableType type : sqlClient.getEntityManager().getAllTypes(sqlClient.getMicroServiceName())) {
            for (ImmutableProp prop : type.getEntityProps().values()) {
                BinLogPropReader reader = reader(prop, propReaderMap);
                if (reader != null) {
                    propNameReaderMap.put(prop.toString(), reader);
                }
            }
        }
        this.readerMap.putAll(propNameReaderMap);
        this.typeReaderMap.putAll(typePropReaderMap);
        return this;
    }

    public JSqlClientImplementor sqlClient() {
        return sqlClient;
    }

    public BinLogPropReader reader(ImmutableProp prop) {
        return readerCache.get(prop);
    }

    public <T> T parseEntity(@NotNull Class<T> type, String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return mapper().readValue(json, type);
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

        MetadataStrategy strategy = sqlClient.getMetadataStrategy();

        if (sourceIdProp.isEmbedded(EmbeddedLevel.SCALAR)) {
            sourceId = Internal.produce(sourceIdProp.getTargetType(), null, draft -> {
                Iterator<Map.Entry<String, JsonNode>> itr = data.fields();
                while (itr.hasNext()) {
                    Map.Entry<String, JsonNode> e = itr.next();
                    List<ImmutableProp> chain = associationType.getPropChain(e.getKey(), strategy);
                    if (chain.get(0) == sourceProp) {
                        ValueParser.addEntityProp(
                                (DraftSpi) draft,
                                chain.subList(2, chain.size()),
                                e.getValue(),
                                this
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
                    List<ImmutableProp> chain = associationType.getPropChain(e.getKey(), strategy);
                    if (chain.get(0) == targetProp) {
                        ValueParser.addEntityProp(
                                (DraftSpi) draft,
                                chain.subList(2, chain.size()),
                                e.getValue(),
                                this
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
                List<ImmutableProp> chain = associationType.getPropChain(e.getKey(), strategy);
                ImmutableProp prop = chain.get(0);
                if (prop == sourceProp) {
                    sourceId = ValueParser.parseSingleValue(
                            this,
                            e.getValue(),
                            sourceIdProp,
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
                            this,
                            e.getValue(),
                            targetIdProp,
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
            data = mapper().readTree(json);
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

    private static BinLogPropReader reader(
            ImmutableProp prop,
            Map<ImmutableProp, BinLogPropReader> configuredReaderMap
    ) {
        BinLogPropReader reader = configuredReaderMap.get(prop);
        if (reader != null) {
            return reader;
        }
        for (ImmutableType superType : prop.getDeclaringType().getSuperTypes()) {
            ImmutableProp superProp = superType.getProps().get(prop.getName());
            if (superProp != null) {
                BinLogPropReader superReader = reader(superProp, configuredReaderMap);
                if (reader == null) {
                    reader = superReader;
                } else if (!reader.equals(superReader)) {
                    throw new ModelException(
                            "Conflict super binlog reader for property \"" +
                                    prop +
                                    "\""
                    );
                }
            }
        }
        return reader;
    }

    private BinLogPropReader createReader(ImmutableProp prop) {
        BinLogPropReader reader = readerMap.get(prop.toString());
        if (reader != null) {
            return reader;
        }
        return typeReaderMap.get(prop.getElementClass());
    }

    private ObjectMapper mapper() {
        ObjectMapper mapper = this.mapper;
        if (mapper == null) {
            throw new IllegalStateException(
                    "The binlog is not ready because the initialization of sqlClient is 'MANUAL' " +
                            "but the sqlClient is not initialized"
            );
        }
        return mapper;
    }
}
