package org.babyfish.jimmer.impl.util;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;

import java.util.Arrays;

public class FlatUtils {

    private FlatUtils() {}

    public static <T> T get(Object immutable, int ... propIds) {
        PropId[] arr = new PropId[propIds.length];
        for (int i = propIds.length - 1; i >= 0; --i) {
            arr[i] = PropId.byIndex(propIds[i]);
        }
        return get(immutable, arr);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Object immutable, PropId ... propIds) {
        ImmutableSpi spi = (ImmutableSpi) immutable;
        for (PropId propId : propIds) {
            if (!spi.__isLoaded(propId)) {
                return null;
            }
            Object value = spi.__get(propId);
            if (!(value instanceof ImmutableSpi)) {
                return (T) value;
            }
            spi = (ImmutableSpi) value;
        }
        return (T) spi;
    }

    public static void set(Draft draft, int[] propIds, Object value) {
        PropId[] arr = new PropId[propIds.length];
        for (int i = propIds.length - 1; i >= 0; --i) {
            arr[i] = PropId.byIndex(propIds[i]);
        }
        set(draft, arr, value);
    }

    public static void set(Draft draft, PropId[] propIds, Object value) {
        if (draft == null) {
            throw new IllegalArgumentException("draft cannot be null");
        }
        if (propIds.length == 0 || value == null) {
            return;
        }
        DraftSpi spi = (DraftSpi) draft;
        int depth = propIds.length;

        DraftSpi[] sources = new DraftSpi[depth];
        ImmutableProp[] props = new ImmutableProp[depth];
        for (int i = 0; i < depth; i++) {
            PropId propId = propIds[i];
            if (spi != null) {
                sources[i] = spi;
                props[i] = spi.__type().getProp(propId);
                if (props[i].getTargetType() != null) {
                    spi = (DraftSpi) (spi.__isLoaded(propId) ? spi.__get(propId) : null);
                } else {
                    spi = null;
                }
            } else {
                props[i] = props[i - 1].getTargetType().getProp(propId);
            }
            if (props[i].getTargetType() == null && i + 1 < depth) {
                throw notAssociation(i);
            }
        }
        for (int i = depth - 1; i >= 0; --i) {
            PropId propId = propIds[i];
            Object deeperValue = value;
            DraftSpi source = sources[i];
            if (source != null) {
                source.__set(propId, value);
                break;
            } else {
                ImmutableType immutableType = i > 0 ? props[i - 1].getTargetType() : ((DraftSpi) draft).__type();
                value = Internal.produce(immutableType, null, d -> {
                    ((DraftSpi)d).__set(propId, deeperValue);
                });
            }
        }
    }

    private static RuntimeException notAssociation(int index) {
        return new IllegalArgumentException("props[" + index + "] is not association");
    }
}
