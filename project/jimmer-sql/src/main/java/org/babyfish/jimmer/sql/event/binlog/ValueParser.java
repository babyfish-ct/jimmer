package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.Temporal;
import java.util.*;

class ValueParser {

    private static final Map<Class<?>, Caster> CASTER_MAP =
            new CasterMapBuilder()
                    .add(boolean.class, Boolean.class, JsonNode::asBoolean)
                    .add(char.class, Character.class, valueNode -> valueNode.asText().charAt(0))
                    .add(byte.class, Byte.class, valueNode -> (byte)valueNode.asInt())
                    .add(short.class, Short.class, valueNode -> (short)valueNode.asInt())
                    .add(int.class, Integer.class, JsonNode::asInt)
                    .add(long.class, Long.class, JsonNode::asLong)
                    .add(float.class, Float.class, valueNode -> (float)valueNode.asDouble())
                    .add(double.class, Double.class, JsonNode::asDouble)
                    .add(BigInteger.class, valueNode -> new BigInteger(valueNode.asText()))
                    .add(BigDecimal.class, valueNode -> new BigDecimal(valueNode.asText()))
                    .add(String.class, JsonNode::asText)
                    .add(UUID.class, valueNode -> UUID.fromString(valueNode.asText()))
                    .build();

    private static final Object ILLEGAL_VALUE = new Object();

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private ValueParser() {}

    public static void addEntityProp(
            DraftSpi spi,
            List<ImmutableProp> chain,
            JsonNode jsonNode,
            JSqlClient sqlClient
    ) throws IOException {
        ImmutableProp entityProp = chain.get(0);
        if (entityProp.isEmbedded(EmbeddedLevel.BOTH)) {
            for (ImmutableProp prop : chain) {
                int propId = prop.getId();
                if (prop.getTargetType() != null) {
                    if (!spi.__isLoaded(propId)) {
                        spi.__set(propId, Internal.produce(prop.getTargetType(), null, null));
                    }
                    spi = (DraftSpi) spi.__get(propId);
                } else {
                    Object value = ValueParser.parseSingleValue(
                            sqlClient,
                            jsonNode,
                            prop.getElementClass(),
                            true
                    );
                    if (value != null || prop.isNullable()) {
                        spi.__set(propId, value);
                    }
                }
            }
        } else {
            Object value;
            if (entityProp.isAssociation(TargetLevel.ENTITY)) {
                ImmutableProp targetIdProp = entityProp.getTargetType().getIdProp();
                Object valueId = ValueParser.parseSingleValue(
                        sqlClient,
                        jsonNode,
                        targetIdProp.getElementClass(),
                        false
                );
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
                value = ValueParser.parseSingleValue(
                        sqlClient,
                        jsonNode,
                        entityProp.getElementClass(),
                        true
                );
            }
            if (value != ILLEGAL_VALUE) {
                spi.__set(entityProp.getId(), value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Object parseSingleValue(
            JSqlClient sqlClient,
            JsonNode jsonNode,
            Class<?> javaType,
            boolean useScalarProvider
    ) {
        ScalarProvider<Object, Object> provider =
                useScalarProvider ?
                        (ScalarProvider<Object, Object>)
                                sqlClient.getScalarProvider(javaType) :
                        null;
        Class<?> sqlType = provider != null ? provider.getSqlType() : javaType;
        if (Date.class.isAssignableFrom(sqlType) || Temporal.class.isAssignableFrom(sqlType)) {
            return ILLEGAL_VALUE;
        }
        Object value = valueOf(jsonNode, sqlType);
        if (provider != null && value != null) {
            return provider.toScalar(value);
        }
        return value;
    }
    
    private static Object valueOf(JsonNode node, Class<?> type) {
        if (node.isNull()) {
            return null;
        }
        Caster caster = CASTER_MAP.get(type);
        if (caster != null) {
            return caster.cast(node);
        }
        try {
            return MAPPER.readValue(node.toString(), type);
        } catch (JsonProcessingException ex) {
            return ILLEGAL_VALUE;
        }
    }

    private static class CasterMapBuilder {

        private Map<Class<?>, Caster> map = new HashMap<>();

        public CasterMapBuilder add(Class<?> type, Caster caster) {
            map.put(type, caster);
            return this;
        }

        public CasterMapBuilder add(Class<?> type1, Class<?> type2, Caster caster) {
            map.put(type1, caster);
            map.put(type2, caster);
            return this;
        }

        public Map<Class<?>, Caster> build() {
            return map;
        }
    }

    private interface Caster {
        Object cast(JsonNode valueNode);
    }
}
