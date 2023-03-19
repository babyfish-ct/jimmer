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
            boolean requiresKey
    ) {
        ImmutableType type = spi.__type();
        if (keyProps == null || keyProps.isEmpty()) {
            if (requiresKey) {
                throw new IllegalArgumentException(
                        "Requires key properties configuration for \"" +
                                type +
                                "\", in an idempotent save command, " +
                                "if the saved associated object does not have id, " +
                                "either configure the key properties for the type of associated object, " +
                                "or set the handle mode of association to `AppendOnly`"
                );
            }
            return null;
        }
        Object[] arr = new Object[keyProps.size()];
        int index = 0;
        for (ImmutableProp keyProp : keyProps) {
            if (!spi.__isLoaded(keyProp.getId())) {
                if (requiresKey) {
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