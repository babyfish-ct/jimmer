package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.jackson.DeserializeUtils;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.util.EmbeddableObjects;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.io.IOException;
import java.time.temporal.Temporal;
import java.util.*;

class BinLogDeserializer extends StdDeserializer<Object> {

    private static final Object ILLEGAL_VALUE = new Object();

    private final JSqlClient sqlClient;

    private final ImmutableType immutableType;

    public BinLogDeserializer(
            JSqlClient sqlClient,
            ImmutableType immutableType
    ) {
        super(immutableType.getJavaClass());
        this.sqlClient = sqlClient;
        this.immutableType = immutableType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserialize(
            JsonParser jp,
            DeserializationContext ctx
    ) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        return Internal.produce(immutableType, null, draft -> {
            Iterator<Map.Entry<String, JsonNode>> itr = node.fields();
            while (itr.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = itr.next();
                String columnName = fieldEntry.getKey();
                JsonNode childNode = fieldEntry.getValue();
                List<ImmutableProp> chain = immutableType.getPropChainByColumnName(columnName);
                ImmutableProp entityProp = chain.get(0);
                if (entityProp.isEmbedded()) {
                    DraftSpi spi = (DraftSpi) draft;
                    for (ImmutableProp prop : chain) {
                        int propId = prop.getId();
                        if (prop.getTargetType() != null) {
                            if (!spi.__isLoaded(propId)) {
                                spi.__set(propId, Internal.produce(prop.getTargetType(), null, null));
                            }
                            spi = (DraftSpi) spi.__get(propId);
                            if (prop.isAssociation(TargetLevel.ENTITY)) {
                                ImmutableProp idProp = prop.getTargetType().getIdProp();
                                int targetIdPropId = idProp.getId();
                                if (!spi.__isLoaded(targetIdPropId)) {
                                    spi.__set(targetIdPropId, Internal.produce(idProp.getTargetType(), null, null));
                                }
                                spi = (DraftSpi) spi.__get(targetIdPropId);
                            }
                        } else {
                            Object value = parseSingleValue(ctx, childNode, prop.getElementClass(), true);
                            if (value != null || prop.isNullable()) {
                                spi.__set(propId, value);
                            }
                        }
                    }
                } else {
                    Object value;
                    if (entityProp.isAssociation(TargetLevel.ENTITY)) {
                        ImmutableProp targetIdProp = entityProp.getTargetType().getIdProp();
                        Object valueId = parseSingleValue(ctx, childNode, targetIdProp.getElementClass(), false);
                        value = valueId == null ?
                                null :
                                Internal.produce(
                                        entityProp.getTargetType(),
                                        null,
                                        targetDraft -> {
                                            ((DraftSpi) targetDraft).__set(
                                                    targetIdProp.getId(),
                                                    valueId
                                            );
                                        }
                                );
                    } else {
                        value = parseSingleValue(ctx, childNode, entityProp.getElementClass(), true);
                        if (value == ILLEGAL_VALUE) {
                            continue;
                        }
                    }
                    ((DraftSpi) draft).__set(entityProp.getId(), value);
                }
            }
            for (ImmutableProp prop : immutableType.getProps().values()) {
                if (prop.isEmbedded()) {
                    if (!EmbeddableObjects.isCompleted(((DraftSpi) draft).__get(prop.getId()))) {
                        if (!prop.isNullable()) {
                            throw new IllegalArgumentException(
                                    "Illegal binlog data, the property \"" + prop + "\" is not nullable"
                            );
                        }
                        ((DraftSpi) draft).__set(prop.getId(), null);
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Object parseSingleValue(
            DeserializationContext ctx,
            JsonNode jsonNode,
            Class<?> javaType,
            boolean useScalarProvider
    ) throws IOException {
        ScalarProvider<Object, Object> provider =
                useScalarProvider ?
                        (ScalarProvider<Object, Object>)
                                sqlClient.getScalarProvider(javaType) :
                null;
        Class<?> sqlType = provider != null ? provider.getSqlType() : javaType;
        if (Date.class.isAssignableFrom(sqlType) || Temporal.class.isAssignableFrom(sqlType)) {
            return ILLEGAL_VALUE;
        }
        Object value = DeserializeUtils.readTreeAsValue(
                ctx,
                jsonNode,
                SimpleType.constructUnsafe(sqlType)
        );
        if (provider != null && value != null) {
            value = provider.toScalar(value);
        }
        return value;
    }
}
