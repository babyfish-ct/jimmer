package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.runtime.ExecutionException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

class Key {

    private Object[] arr;

    private Key(Object[] arr) {
        this.arr = arr;
    }

    public List<Object> toList() {
        return Arrays.asList(arr.clone());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key key = (Key) o;
        return Arrays.equals(arr, key.arr);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(arr);
    }

    public static Key of(
            AbstractEntitySaveCommandImpl.Data data,
            ImmutableSpi spi,
            boolean force
    ) {
        ImmutableType type = spi.__type();
        Set<ImmutableProp> keyProps = data.getKeyProps(type);
        if (keyProps == null) {
            if (force) {
                throw new ExecutionException(
                        "Requires key properties configuration for \"" +
                                type +
                                "\", because there are some objects without id need to be handled"
                );
            }
            return null;
        }
        Object[] arr = new Object[keyProps.size()];
        int index = 0;
        for (ImmutableProp keyProp : keyProps) {
            if (!spi.__isLoaded(keyProp.getName())) {
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
            Object value = spi.__get(keyProp.getName());
            if (value == null) {
                throw new ExecutionException(
                        "The key property \"" +
                                keyProp.getName() +
                                "\" of \"" +
                                type +
                                "\" cannot be null"
                );
            }
            arr[index++] = value;
        }
        return new Key(arr);
    }
}