package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.runtime.ExecutionException;

import java.util.Arrays;
import java.util.Set;

class TypedKey {

    private final ImmutableType type;

    private final Object[] arr;

    private TypedKey(ImmutableType type, Object[] arr) {
        this.type = type;
        this.arr = arr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypedKey key = (TypedKey) o;
        return type == key.type && Arrays.equals(arr, key.arr);
    }

    @Override
    public int hashCode() {
        return type.hashCode() ^ Arrays.hashCode(arr);
    }

    public static TypedKey of(
            ImmutableSpi spi,
            Set<ImmutableProp> keyProps,
            boolean force
    ) {
        ImmutableType type = spi.__type();
        if (keyProps == null || keyProps.isEmpty()) {
            if (force) {
                throw new ExecutionException(
                        "Requires key properties configuration for \"" +
                                type +
                                "\", In an idempotent save command, " +
                                "if the saved object does not have id, " +
                                "its key property must be specified."
                );
            }
            return null;
        }
        Object[] arr = new Object[keyProps.size()];
        int index = 0;
        for (ImmutableProp keyProp : keyProps) {
            if (!spi.__isLoaded(keyProp.getId())) {
                if (force) {
                    throw new ExecutionException(
                            "The key property \"" +
                                    keyProp.getName() +
                                    "\" of \"" +
                                    type +
                                    "\" cannot be unloaded when the id is not specified"
                    );
                }
                return null;
            }
            Object value = spi.__get(keyProp.getId());
            arr[index++] = value;
        }
        return new TypedKey(type, arr);
    }
}