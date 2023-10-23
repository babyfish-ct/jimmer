package org.babyfish.jimmer.sql.cache;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

public class ParameterMaps {

    public static <String, Object> SortedMap<String, Object> of() {
        return Collections.emptySortedMap();
    }

    public static <String, Object> SortedMap<String, Object> of(String key, Object value) {
        if (value == null) {
            return Collections.emptySortedMap();
        }
        SortedMap<String, Object> map = new TreeMap<>();
        map.put(key, value);
        return map;
    }

    public static <String, Object> SortedMap<String, Object> of(
            String key1, Object value1, String key2, Object value2
    ) {
        if (value1 == null && value2 == null) {
            return Collections.emptySortedMap();
        }
        SortedMap<String, Object> map = new TreeMap<>();
        if (value1 != null) {
            map.put(key1, value1);
        }
        if (value2 != null) {
            map.put(key2, value2);
        }
        return map;
    }

    public static <String, Object> SortedMap<String, Object> of(
            String key1, Object value1,
            String key2, Object value2,
            String key3, Object value3
    ) {
        if (value1 == null && value2 == null && value3 == null) {
            return Collections.emptySortedMap();
        }
        SortedMap<String, Object> map = new TreeMap<>();
        if (value1 != null) {
            map.put(key1, value1);
        }
        if (value2 != null) {
            map.put(key2, value2);
        }
        if (value3 != null) {
            map.put(key3, value3);
        }
        return map;
    }
}
