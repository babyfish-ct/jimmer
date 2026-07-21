package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

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
        if (entities.isEmpty() ||
                fetcher == null && !immutableType.hasDerivedTypes()) {
            return;
        }
        Map<ImmutableType, Shape> shapeMap = new HashMap<>();
        boolean needReshape = false;
        for (ImmutableSpi spi : (List<ImmutableSpi>) entities) {
            ImmutableType actualType = spi.__type();
            Shape shape = shapeMap.computeIfAbsent(actualType, it -> createShape(immutableType, fetcher, it));
            if (!needReshape) {
                needReshape = shape.requiresReshape();
            }
        }
        if (needReshape) {
            entities.replaceAll(entity -> {
                ImmutableSpi spi = (ImmutableSpi) entity;
                Shape shape = shapeMap.get(spi.__type());
                return (E) Internal.produce(spi.__type(), spi, draft -> shape.apply((DraftSpi) draft));
            });
        }
        if (fetcher != null) {
            FetcherUtil.fetch(sqlClient, con, fetcher, converter, entities);
        }
    }

    private static Shape createShape(
            ImmutableType immutableType,
            Fetcher<?> fetcher,
            ImmutableType actualType
    ) {
        Map<String, Field> fieldMap = fetcher != null ? fieldMap(fetcher, actualType) : null;
        Map<String, ImmutableProp> objectCachePropMap = fetcher == null ? immutableType.getObjectCacheProps() : null;
        Map<String, ImmutableProp> actualObjectCachePropMap = actualType.getObjectCacheProps();
        List<PropId> unloadPropIds = new ArrayList<>();
        List<PropId> hidePropIds = new ArrayList<>();
        List<PropId> showPropIds = new ArrayList<>();
        for (ImmutableProp prop : actualType.getProps().values()) {
            if (!isAvailableInObjectCache(prop, actualObjectCachePropMap)) {
                continue;
            }
            Field field = fieldMap != null ? fieldMap.get(prop.getName()) : null;
            boolean included = fieldMap != null ?
                    field != null :
                    objectCachePropMap.containsKey(prop.getName());
            if (!included) {
                if (prop.isView()) {
                    hidePropIds.add(prop.getId());
                } else {
                    unloadPropIds.add(prop.getId());
                }
            } else if (field != null && field.isImplicit()) {
                hidePropIds.add(prop.getId());
            }
        }
        if (fieldMap != null) {
            for (Field field : fieldMap.values()) {
                ImmutableProp prop = field.getProp();
                if (prop.isView() || prop.isFormula()) {
                    showPropIds.add(prop.getId());
                }
            }
        }
        return new Shape(unloadPropIds, hidePropIds, showPropIds);
    }

    private static boolean isAvailableInObjectCache(
            ImmutableProp prop,
            Map<String, ImmutableProp> objectCachePropMap
    ) {
        if (objectCachePropMap.containsKey(prop.getName())) {
            return true;
        }
        ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
        return idViewBaseProp != null && objectCachePropMap.containsKey(idViewBaseProp.getName());
    }

    private static Map<String, Field> fieldMap(Fetcher<?> fetcher, ImmutableType actualType) {
        Map<String, Field> fieldMap = new HashMap<>();
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

    private static class Shape {

        private final List<PropId> unloadPropIds;

        private final List<PropId> hidePropIds;

        private final List<PropId> showPropIds;

        private Shape(
                List<PropId> unloadPropIds,
                List<PropId> hidePropIds,
                List<PropId> showPropIds
        ) {
            this.unloadPropIds = unloadPropIds;
            this.hidePropIds = hidePropIds;
            this.showPropIds = showPropIds;
        }

        private boolean requiresReshape() {
            return !unloadPropIds.isEmpty() || !hidePropIds.isEmpty();
        }

        private void apply(DraftSpi draft) {
            // These operations are idempotent for unloaded properties, so the same
            // target shape can be applied to cache hits and freshly loaded values.
            for (PropId propId : unloadPropIds) {
                draft.__unload(propId);
            }
            for (PropId propId : hidePropIds) {
                draft.__show(propId, false);
            }
            for (PropId propId : showPropIds) {
                draft.__show(propId, true);
            }
        }
    }
}
