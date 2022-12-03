package org.babyfish.jimmer.sql.ast.impl.util;

import org.babyfish.jimmer.impl.util.StaticCache;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmbeddableObjects {

    private static final StaticCache<ImmutableType, List<Class<?>>> FLAT_TYPES_CACHE =
            new StaticCache<>(EmbeddableObjects::createFlatTypes, false);

    private EmbeddableObjects() {}

    public static List<Class<?>> expandTypes(ImmutableType type) {
        return FLAT_TYPES_CACHE.get(type);
    }

    public static Object[] expand(ImmutableType type, Object obj) {
        if (obj != null && !type.getJavaClass().isAssignableFrom(obj.getClass())) {
            throw new IllegalArgumentException(
                    "Illegal obj, it does not match the type \"" +
                            type +
                            "\""
            );
        }
        Object[] values = new Object[FLAT_TYPES_CACHE.get(type).size()];
        if (obj != null) {
            expandImpl((ImmutableSpi) obj, values, 0);
        }
        return values;
    }

    private static int expandImpl(ImmutableSpi spi, Object[] values, int index) {
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            Object value = spi.__get(prop.getId());
            if (prop.isEmbedded()) {
                index = expandImpl((ImmutableSpi) value, values, index);
            } else {
                values[index++] = value;
            }
        }
        return index;
    }

    private static List<Class<?>> createFlatTypes(ImmutableType type) {
        if (!type.isEmbeddable()) {
            throw new IllegalArgumentException(
                    "Illegal obj, it does not match the type \"" +
                            type +
                            "\""
            );
        }
        List<Class<?>> flatTypes = new ArrayList<>();
        collectFlatTypes(type, flatTypes);
        return Collections.unmodifiableList(flatTypes);
    }

    private static void collectFlatTypes(ImmutableType type, List<Class<?>> flatTypes) {
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.isEmbedded()) {
                collectFlatTypes(prop.getTargetType(), flatTypes);
            } else {
                flatTypes.add(prop.getElementClass());
            }
        }
    }
}
