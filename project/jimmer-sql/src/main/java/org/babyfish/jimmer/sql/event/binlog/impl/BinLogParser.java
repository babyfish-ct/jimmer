package org.babyfish.jimmer.sql.event.binlog.impl;

import org.babyfish.jimmer.impl.util.PropCache;
import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.jackson.codec.Node;
import org.babyfish.jimmer.lang.Lazy;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.association.meta.AssociationProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.binlog.BinLogPropReader;
import org.babyfish.jimmer.sql.meta.JoinTableFilterInfo;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BinLogParser {

    private final Map<String, BinLogPropReader> readerMap = new HashMap<>();

    private final Map<Class<?>, BinLogPropReader> typeReaderMap;

    private final PropCache<BinLogPropReader> readerCache = new PropCache<>(this::createReader, true);

    private final JsonCodec<?> jsonCodec;

    private final Map<ImmutableProp, BinLogPropReader> configuredReaderMap;

    private JSqlClientImplementor sqlClient;

    public BinLogParser(
            @NotNull JsonCodec<?> jsonCodec,
            @NotNull Map<ImmutableProp, BinLogPropReader> propReaderMap,
            @NotNull Map<Class<?>, BinLogPropReader> typePropReaderMap
    ) {
        this.jsonCodec = jsonCodec.withCustomizations(new BinLogModuleCustomization(this));
        this.configuredReaderMap = propReaderMap;
        this.typeReaderMap = typePropReaderMap;
    }

    public BinLogParser initialize(
            JSqlClientImplementor sqlClient
    ) {
        if (sqlClient == null) {
            throw new IllegalArgumentException("`sqlClient` cannot be null");
        }
        this.sqlClient = sqlClient;
        Map<String, BinLogPropReader> propNameReaderMap = new HashMap<>();
        for (ImmutableType type : sqlClient.getEntityManager().getAllTypes(sqlClient.getMicroServiceName())) {
            for (ImmutableProp prop : type.getEntityProps().values()) {
                BinLogPropReader reader = reader(prop, configuredReaderMap);
                if (reader != null) {
                    propNameReaderMap.put(prop.toString(), reader);
                }
            }
        }
        this.readerMap.putAll(propNameReaderMap);
        return this;
    }

    public JSqlClientImplementor sqlClient() {
        return sqlClient;
    }

    public BinLogPropReader reader(ImmutableProp prop) {
        return readerCache.get(prop);
    }

    JsonCodec<?> codec() {
        return jsonCodec;
    }

    public <T> T parseEntity(@NotNull Class<T> type, String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return jsonCodec.readerFor(type).read(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Illegal json: " + json, ex);
        }
    }

    public <T> T parseEntity(@NotNull Class<T> type, Node data) {
        if (data == null || data.isNull()) {
            return null;
        }
        return parseEntity(type, data.toString());
    }

    @SuppressWarnings("unchecked")
    public <T> T parseEntity(@NotNull ImmutableType type, Node data) {
        if (type instanceof AssociationType) {
            throw new IllegalArgumentException("type cannot be AssociationType");
        }
        return (T) parseEntity(type.getJavaClass(), data);
    }

    @SuppressWarnings("unchecked")
    public <S, T> MiddleRow<S, T> parseMiddleRow(@NotNull AssociationType associationType, Node data) {
        AssociationProp sourceProp = associationType.getSourceProp();
        AssociationProp targetProp = associationType.getTargetProp();
        ImmutableProp sourceIdProp = sourceProp.getTargetType().getIdProp();
        ImmutableProp targetIdProp = targetProp.getTargetType().getIdProp();
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        LogicalDeletedInfo deletedInfo = associationType.getJoinTableDeletedInfo();
        JoinTableFilterInfo filterInfo = associationType.getJoinTableFilterInfo();
        String deletedColumnName = deletedInfo != null ?
                DatabaseIdentifiers.comparableIdentifier(deletedInfo.getColumnName()) :
                null;
        String filteredColumnName = filterInfo != null ?
                DatabaseIdentifiers.comparableIdentifier(filterInfo.getColumnName()) :
                null;

        Object sourceId = null;
        Object targetId = null;
        Boolean deleted = null;
        Object filteredValue = null;

        // Low version jackson does not support `properties`
        Iterator<Map.Entry<String, Node>> fieldEntryItr = data.fieldsIterator();
        while (fieldEntryItr.hasNext()) {
            Map.Entry<String, Node> e = fieldEntryItr.next();
            String columnName = e.getKey();
            String comparableColumnName = DatabaseIdentifiers.comparableIdentifier(columnName);
            if (comparableColumnName.equals(deletedColumnName)) {
                deleted = deletedInfo.isDeleted(ValueParser.valueOrError(this.codec(), e.getValue(), deletedInfo.getType()));
                continue;
            }
            if (comparableColumnName.equals(filteredColumnName)) {
                filteredValue = ValueParser.valueOrError(this.codec(), e.getValue(), filterInfo.getType());
                continue;
            }
            List<ImmutableProp> chain = associationType.getPropChain(e.getKey(), strategy, true);
            if (chain == null) {
                continue;
            }
            if (chain.get(0) == sourceProp) {
                if (sourceIdProp.isEmbedded(EmbeddedLevel.SCALAR)) {
                    sourceId = Internal.produce(sourceIdProp.getTargetType(), sourceId, draft -> {
                        ValueParser.addEntityProp(
                                (DraftSpi) draft,
                                chain.subList(2, chain.size()),
                                e.getValue(),
                                this
                        );
                    });
                } else {
                    sourceId = ValueParser.parseSingleValue(
                            this,
                            e.getValue(),
                            sourceIdProp,
                            false
                    );
                }
            } else if (chain.get(0) == targetProp) {
                if (targetIdProp.isEmbedded(EmbeddedLevel.SCALAR)) {
                    targetId = Internal.produce(targetIdProp.getTargetType(), targetId, draft -> {
                        ValueParser.addEntityProp(
                                (DraftSpi) draft,
                                chain.subList(2, chain.size()),
                                e.getValue(),
                                this
                        );
                    });
                } else {
                    targetId = ValueParser.parseSingleValue(
                            this,
                            e.getValue(),
                            targetIdProp,
                            false
                    );
                }
            }
        }
        return new MiddleRow<>((S) sourceId, (T) targetId, deleted, filteredValue);
    }

    public <S, T> MiddleRow<S, T> parseMiddleRow(@NotNull AssociationType associationType, String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        Node data;
        try {
            data = jsonCodec.treeReader().read(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Illegal json: " + json, ex);
        }
        return parseMiddleRow(associationType, data);
    }

    public <S, T> MiddleRow<S, T> parseMiddleRow(@NotNull TypedProp<?, ?> prop, Node data) {
        return parseMiddleRow(AssociationType.of(prop.unwrap()), data);
    }

    public <S, T> MiddleRow<S, T> parseMiddleRow(@NotNull ImmutableProp prop, Node data) {
        return parseMiddleRow(AssociationType.of(prop), data);
    }

    public <S, T> MiddleRow<S, T> parseMiddleRow(@NotNull TypedProp<?, ?> prop, String json) {
        return parseMiddleRow(AssociationType.of(prop.unwrap()), json);
    }

    public <S, T> MiddleRow<S, T> parseMiddleRow(
            @NotNull ImmutableProp prop,
            String json,
            @Nullable Lazy<Tuple2<?, ?>> defaultTupleLazy
    ) {
        return parseMiddleRow(AssociationType.of(prop), json);
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

}
