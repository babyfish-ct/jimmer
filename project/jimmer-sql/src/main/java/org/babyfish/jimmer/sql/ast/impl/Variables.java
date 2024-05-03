package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.impl.util.ArrayUtils;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.time.*;
import java.util.Arrays;
import java.util.Collection;

public class Variables {

    public static Object process(
            @Nullable Object value,
            @NotNull ImmutableProp prop,
            @NotNull JSqlClientImplementor sqlClient
    ) {
        return process(value, prop, true, sqlClient);
    }

    @SuppressWarnings("unchecked")
    public static Object process(
            @Nullable Object value,
            @NotNull ImmutableProp prop,
            boolean applyScalarProvider,
            @NotNull JSqlClientImplementor sqlClient
    ) {
        if (value instanceof DbLiteral) {
            return value;
        }
        if (prop.isReference(TargetLevel.ENTITY)) {
            if (value != null) {
                value = ((ImmutableSpi) value).__get(prop.getTargetType().getIdProp().getId());
            }
            prop = prop.getTargetType().getIdProp();
        }
        if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            return new DbLiteral.DbValue(prop, handleDateTime(value, sqlClient.getZoneId()), false);
        }
        if (applyScalarProvider) {
            ScalarProvider<Object, Object> scalarProvider = sqlClient.getScalarProvider(prop);
            if (scalarProvider != null && value != null) {
                try {
                    value = scalarProvider.toSql(value);
                } catch (Exception ex) {
                    throw new ExecutionException(
                            "The value \"" +
                                    value +
                                    "\" cannot be converted by the scalar provider \"" +
                                    scalarProvider +
                                    "\""
                    );
                }
            }
            if (value == null) {
                return new DbLiteral.DbNull(
                        scalarProvider != null ?
                                scalarProvider.getSqlType() :
                                prop.getReturnClass()
                        );
            }
            if (scalarProvider != null) {
                return scalarProvider.isJsonScalar() ?
                        new DbLiteral.DbValue(prop, value, true) :
                        handleDateTime(value, sqlClient.getZoneId());
            }
        }
        if (value == null) {
            return new DbLiteral.DbNull(prop.getReturnClass());
        }
        if (value instanceof Collection<?> && prop.isScalar(TargetLevel.ENTITY)) {
            Object[] arr = (Object[]) Array.newInstance(Classes.boxTypeOf(prop.getElementClass()), ((Collection<?>) value).size());
            ((Collection<Object>) value).toArray(arr);
            value = arr;
        }
        if (value.getClass().isArray()) {
            Storage storage = prop.getStorage(sqlClient.getMetadataStrategy());
            if (storage instanceof SingleColumn) {
                SingleColumn singleColumn = (SingleColumn) storage;
                if (singleColumn.getSqlElementType() != null) {
                    value = new TypedList<>(singleColumn.getSqlElementType(), ArrayUtils.toObject(value));
                }
            }
        }
        return handleDateTime(value, sqlClient.getZoneId());
    }

    private static Object handleDateTime(Object value, ZoneId zoneId) {
        if (value instanceof Instant) {
            return Timestamp.from((Instant) value);
        }
        if (value instanceof LocalDateTime) {
            return Timestamp.from(((LocalDateTime)value).atZone(zoneId).toInstant());
        }
        if (value instanceof LocalDate) {
            return java.sql.Date.valueOf((LocalDate)value);
        }
        if (value instanceof LocalTime) {
            return java.sql.Time.valueOf((LocalTime) value);
        }
        if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date)value).getTime());
        }
        return value;
    }
}
