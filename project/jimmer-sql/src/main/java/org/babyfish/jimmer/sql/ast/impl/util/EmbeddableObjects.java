package org.babyfish.jimmer.sql.ast.impl.util;

import org.babyfish.jimmer.impl.util.StaticCache;
import org.babyfish.jimmer.meta.EmbeddedLevel;
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
            if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
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
            if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                collectFlatTypes(prop.getTargetType(), flatTypes);
            } else {
                flatTypes.add(prop.getElementClass());
            }
        }
    }

    public static boolean isCompleted(Object embedded) {
        if (!(embedded instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("The argument must be embeddable type");
        }
        return isCompleted((ImmutableSpi) embedded);
    }

    private static boolean isCompleted(ImmutableSpi spi) {
        ImmutableType type = spi.__type();
        if (type.isEntity()) {
            return isCompleted(spi, type.getIdProp());
        }
        for (ImmutableProp prop : type.getProps().values()) {
            if (!isCompleted(spi, prop)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCompleted(ImmutableSpi spi, ImmutableProp prop) {
        int propId = prop.getId();
        if (!spi.__isLoaded(propId)) {
            return false;
        }
        if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            ImmutableSpi childSpi = (ImmutableSpi) spi.__get(propId);
            if (childSpi != null && !isCompleted(childSpi)) {
                return false;
            }
        }
        return true;
    }
}
