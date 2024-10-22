package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.util.*;

class Keys {

    private Keys() {}

    static Object keyOf(ImmutableSpi spi, Collection<ImmutableProp> keyProps) {
        if (keyProps.size() == 1) {
            ImmutableProp keyProp = keyProps.iterator().next();
            Object o = spi.__get(keyProp.getId());
            if (o != null && keyProp.isReference(TargetLevel.PERSISTENT)) {
                o = ((ImmutableSpi)o).__get(keyProp.getTargetType().getIdProp().getId());
            }
            return o;
        }
        Object[] arr = new Object[keyProps.size()];
        int index = 0;
        for (ImmutableProp keyProp : keyProps) {
            Object o = spi.__get(keyProp.getId());
            if (o != null && keyProp.isReference(TargetLevel.PERSISTENT)) {
                o = ((ImmutableSpi)o).__get(keyProp.getTargetType().getIdProp().getId());
            }
            arr[index++] = o;
        }
        return Tuples.valueOf(arr);
    }
}
