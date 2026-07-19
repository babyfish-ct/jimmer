package org.babyfish.jimmer.sql.event.binlog.impl;

import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.jackson.codec.Node;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.event.binlog.BinLogPropReader;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;

import static org.babyfish.jimmer.sql.ScalarProviderUtils.getSqlType;

class ValueParser {
    private static final Object ILLEGAL_VALUE = new Object();

    private ValueParser() {
    }

    public static void addEntityProp(
            DraftSpi spi,
            List<ImmutableProp> chain,
            Node node,
            BinLogParser parser
    ) {
        ImmutableProp entityProp = chain.get(0);
        if (entityProp.isEmbedded(EmbeddedLevel.BOTH)) {
            for (ImmutableProp prop : chain) {
                PropId propId = prop.getId();
                if (prop.getTargetType() != null) {
                    if (!spi.__isLoaded(propId)) {
                        spi.__set(propId, Internal.produce(prop.getTargetType(), null, null));
                    }
                    spi = (DraftSpi) spi.__get(propId);
                } else {
                    Object value = ValueParser.parseSingleValue(
                            parser,
                            node,
                            prop,
                            true
                    );
                    if (value != null || prop.isNullable()) {
                        spi.__set(propId, value);
                    }
                }
            }
        } else {
            Object value;
            if (entityProp.isAssociation(TargetLevel.PERSISTENT)) {
                ImmutableProp targetIdProp = entityProp.getTargetType().getIdProp();
                Object valueId = ValueParser.parseSingleValue(
                        parser,
                        node,
                        targetIdProp,
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
                        parser,
                        node,
                        entityProp,
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
            BinLogParser parser,
            Node node,
            ImmutableProp prop,
            boolean useScalarProvider
    ) {
        if (node.isNull()) {
            return null;
        }
        BinLogPropReader reader = parser.reader(prop);
        if (reader != null) {
            return reader.read(prop, node);
        }
        Class<?> javaType = prop.getElementClass();
        ScalarProvider<Object, Object> provider =
                useScalarProvider ?
                        (ScalarProvider<Object, Object>)
                                parser.sqlClient().getScalarProvider(javaType) :
                        null;
        Class<?> sqlType = provider != null ?
                getSqlType(provider, parser.sqlClient().getDialect()) :
                javaType;
        if (Date.class.isAssignableFrom(sqlType) || Temporal.class.isAssignableFrom(sqlType)) {
            return ILLEGAL_VALUE;
        }
        Object value = valueOf(parser.codec(), node, sqlType);
        if (provider != null && value != null) {
            try {
                return provider.toScalar(value);
            } catch (Exception ex) {
                throw new ExecutionException(
                        "Cannot convert the value \"" +
                                value +
                                "\" to the jvm type \"" +
                                provider.getScalarType() +
                                "\"",
                        ex
                );
            }
        }
        return value;
    }

    static Object valueOrError(JsonCodec<?> codec, Node node, Class<?> type) {
        if (node.isNull()) {
            return null;
        }
        if (node.canCastTo(type)) {
            return node.castTo(type);
        }
        try {
            return node.convertTo(type, codec.converter());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot convert  \"" +
                    node +
                    "\" to value whose type is \"" +
                    type.getName() +
                    "\""
            );
        }
    }

    private static Object valueOf(JsonCodec<?> codec, Node node, Class<?> type) {
        if (node.isNull()) {
            return null;
        }
        if (node.canCastTo(type)) {
            return node.castTo(type);
        }
        try {
            return node.convertTo(type, codec.converter());
        } catch (Exception ex) {
            return ILLEGAL_VALUE;
        }
    }
}
