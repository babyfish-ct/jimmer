package org.babyfish.jimmer.impl.util;

import java.util.LinkedHashMap;
import java.util.Map;

class LRUMap<K, V> extends LinkedHashMap<K, V> {

    LRUMap() {
        super((128 * 4 + 2) / 3, .75F, true);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return true;
    }
}