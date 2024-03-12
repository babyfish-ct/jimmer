package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.lang.reflect.Array;
import java.util.Collection;

public class Variables {

    public static Object process(Object value, ImmutableProp prop, JSqlClientImplementor sqlClient) {
        return process(value, prop, true, sqlClient);
    }

    @SuppressWarnings("unchecked")
    public static Object process(Object value, ImmutableProp prop, boolean applyScalarProvider, JSqlClientImplementor sqlClient) {
        if (value != null && prop != null && prop.isReference(TargetLevel.ENTITY)) {
            value = ((ImmutableSpi) value).__get(prop.getTargetType().getIdProp().getId());
        }
        ScalarProvider<Object, Object> scalarProvider = null;
        if (applyScalarProvider) {
            if (prop != null) {
                scalarProvider = sqlClient.getScalarProvider(prop);
                if (scalarProvider == null) {
                    scalarProvider = (ScalarProvider<Object, Object>) sqlClient.getScalarProvider(prop.getReturnClass());
                }
            }
            if (scalarProvider == null && value != null) {
                scalarProvider = (ScalarProvider<Object, Object>) sqlClient.getScalarProvider(value.getClass());
            }
        }
        if (scalarProvider != null) {
            if (value == null) {
                value = new DbLiteral.DbNull(scalarProvider.getSqlType());
            } else {
                try {
                    value = scalarProvider.toSql(value);
                } catch (Exception ex) {
                    throw new ExecutionException(
                            "Cannot convert the value \"" +
                                    value +
                                    "\" by the scalar provider \"" +
                                    scalarProvider.getClass().getName() +
                                    "\"",
                            ex
                    );
                }
                if (scalarProvider.isJsonScalar()) {
                    String suffix = sqlClient.getDialect().getJsonLiteralSuffix();
                    if (suffix != null) {
                        value = new DbLiteral.JsonWithSuffix(value, suffix);
                    }
                }
            }
        }
        if (value instanceof Collection<?>) {
            if (prop != null) {
                Object[] arr = (Object[]) Array.newInstance(Classes.boxTypeOf(prop.getElementClass()), ((Collection<?>)value).size());
                ((Collection<Object>)value).toArray(arr);
                value = arr;
            } else {
                value = ((Collection<?>) value).toArray();
            }
        }
        if (prop != null && value != null && value.getClass().isArray()) {
            Storage storage = prop.getStorage(sqlClient.getMetadataStrategy());
            if (storage instanceof SingleColumn) {
                SingleColumn singleColumn = (SingleColumn) storage;
                if (singleColumn.getSqlElementType() != null) {
                    value = new TypedList<>(singleColumn.getSqlElementType(), (Object[]) value);
                }
            }
        }
        return value;
    }
}
