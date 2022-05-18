package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.meta.Column;

import java.util.HashMap;
import java.util.Map;

public class DataCache {

    private static final Object NULL = new Object();

    private Map<Field, Map<Object, Object>> map = new HashMap<>();

    public Object createKey(Field field, ImmutableSpi owner) {
        ImmutableProp prop = field.getProp();
        if (prop.getStorage() instanceof Column) {
            return Ids.idOf((ImmutableSpi) owner.__get(prop.getName()));
        }
        return Ids.idOf(owner);
    }

    public Object get(Field field, Object key) {
        Map<Object, Object> subMap = map.get(field);
        if (subMap == null) {
            return null;
        }
        return subMap.get(key);
    }

    public void put(Field field, Object key, Object value) {
        Map<Object, Object> subMap = map.computeIfAbsent(field, it -> new HashMap<>());
        subMap.put(key, value != null ? value : NULL);
    }

    public static Object unwrap(Object value) {
        return value == NULL ? null : value;
    }
}
