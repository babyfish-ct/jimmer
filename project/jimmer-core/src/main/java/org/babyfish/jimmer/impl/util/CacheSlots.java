package org.babyfish.jimmer.impl.util;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

final class CacheSlots<K, V> {

    private static final Object NULL = new Object();

    private final AtomicReferenceArray<Object> values;

    private final Function<K, V> creator;

    private final boolean nullable;

    CacheSlots(int slotCount, Function<K, V> creator, boolean nullable) {
        values = new AtomicReferenceArray<>(slotCount);
        this.creator = creator;
        this.nullable = nullable;
    }

    @SuppressWarnings("unchecked")
    V get(K key, int slot) {
        Object value = values.get(slot);
        if (value == null) {
            V created = creator.apply(key);
            if (created == null && !nullable) {
                throw new IllegalStateException(
                        "The creator cannot return null because current cache does not accept null values"
                );
            }
            Object newValue = created != null ? created : NULL;
            if (values.compareAndSet(slot, null, newValue)) {
                value = newValue;
            } else {
                value = values.get(slot);
            }
        }
        return value != NULL ? (V) value : null;
    }
}
