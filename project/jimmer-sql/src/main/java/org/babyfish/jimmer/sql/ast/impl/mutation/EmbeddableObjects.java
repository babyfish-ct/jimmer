package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;

public class EmbeddableObjects {

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
            if (prop.isColumnDefinition() && !isCompleted(spi, prop)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCompleted(ImmutableSpi spi, ImmutableProp prop) {
        PropId propId = prop.getId();
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
