package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;

public class Shapes {

    private Shapes() {
    }

    @SuppressWarnings("unchecked")
    public static <E> void reshape(
            JSqlClientImplementor sqlClient,
            Connection con,
            List<E> entities,
            ImmutableType immutableType,
            Fetcher<?> fetcher,
            Function<?, E> converter
    ) {
        if (entities.isEmpty()) {
            return;
        }
        Map<ImmutableType, Map<String, Field>> fieldMapCache = new HashMap<>();
        boolean needDrop = false;
        for (ImmutableSpi spi : (List<ImmutableSpi>) entities) {
            ImmutableType actualType = spi.__type();
            Map<String, Field> fieldMap = fetcher != null ?
                    fieldMapCache.computeIfAbsent(actualType, it -> fieldMap(fetcher, it)) :
                    null;
            for (ImmutableProp prop : actualType.getProps().values()) {
                if (spi.__isLoaded(prop.getId())) {
                    if (!isIncluded(immutableType, fieldMap, prop) || isImplicit(fieldMap, prop)) {
                        needDrop = true;
                        break;
                    }
                }
            }
        }
        if (needDrop) {
            ListIterator<ImmutableSpi> itr = (ListIterator<ImmutableSpi>) entities.listIterator();
            while (itr.hasNext()) {
                ImmutableSpi spi = itr.next();
                Map<String, Field> fieldMap = fetcher != null ? fieldMapCache.get(spi.__type()) : null;
                itr.set(
                        (ImmutableSpi) Internal.produce(spi.__type(), spi, draft -> {
                            for (ImmutableProp prop : spi.__type().getProps().values()) {
                                if (spi.__isLoaded(prop.getId())) {
                                    if (!isIncluded(immutableType, fieldMap, prop)) {
                                        if (!prop.isView()) {
                                            ((DraftSpi) draft).__unload(prop.getId());
                                        } else {
                                            ((DraftSpi) draft).__show(prop.getId(), false);
                                        }
                                    } else if (isImplicit(fieldMap, prop)) {
                                        ((DraftSpi) draft).__show(prop.getId(), false);
                                    }
                                }
                            }
                            if (fieldMap != null) {
                                for (Field field : fieldMap.values()) {
                                    ImmutableProp prop = field.getProp();
                                    if (prop.isView() || prop.isFormula()) {
                                        ((DraftSpi) draft).__show(prop.getId(), true);
                                    }
                                }
                            }
                        })
                );
            }
        }
        if (fetcher != null) {
            FetcherUtil.fetch(
                    sqlClient,
                    con,
                    Collections.singletonList(
                            new FetcherSelection<E>() {

                                @Override
                                public FetchPath getPath() {
                                    return null;
                                }

                                @Override
                                public Fetcher<?> getFetcher() {
                                    return fetcher;
                                }

                                @Override
                                public PropExpression.Embedded<?> getEmbeddedPropExpression() {
                                    return null;
                                }

                                @Override
                                public @Nullable Function<?, E> getConverter() {
                                    return converter;
                                }
                            }
                    ),
                    null,
                    entities
            );
        }
    }

    private static boolean isIncluded(
            ImmutableType immutableType,
            Map<String, Field> fieldMap,
            ImmutableProp prop
    ) {
        return fieldMap != null ?
                fieldMap.containsKey(prop.getName()) :
                immutableType.getObjectCacheProps().containsKey(prop.getName());
    }

    private static boolean isImplicit(Map<String, Field> fieldMap, ImmutableProp prop) {
        if (fieldMap == null) {
            return false;
        }
        Field field = fieldMap.get(prop.getName());
        return field != null && field.isImplicit();
    }

    private static Map<String, Field> fieldMap(Fetcher<?> fetcher, ImmutableType actualType) {
        Map<String, Field> fieldMap = new LinkedHashMap<>();
        collectFields(fetcher, actualType, fieldMap);
        return fieldMap;
    }

    private static void collectFields(
            Fetcher<?> fetcher,
            ImmutableType actualType,
            Map<String, Field> fieldMap
    ) {
        for (Field field : fetcher.getFieldMap().values()) {
            Field oldField = fieldMap.get(field.getProp().getName());
            if (oldField == null || oldField.isImplicit() || !field.isImplicit()) {
                fieldMap.put(field.getProp().getName(), field);
            }
        }
        if (fetcher instanceof FetcherImplementor<?>) {
            for (Map.Entry<ImmutableType, Fetcher<?>> e :
                    ((FetcherImplementor<?>) fetcher).__getTypeBranchFetcherMap().entrySet()) {
                if (e.getKey().isAssignableFrom(actualType)) {
                    collectFields(e.getValue(), actualType, fieldMap);
                }
            }
        }
    }
}
