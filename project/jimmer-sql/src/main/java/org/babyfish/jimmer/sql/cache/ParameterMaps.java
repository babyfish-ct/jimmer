package org.babyfish.jimmer.sql.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Helper class only for java
 */
public class ParameterMaps {

    private static final SortedMap<String, Object> EMPTY = Collections.emptySortedMap();

    private ParameterMaps() {}

    @NotNull
    public static SortedMap<String, Object> of() {
        return EMPTY;
    }

    /**
     * Create parameter map.
     *
     * <ul>
     *     <li>If the parameter `value` is null, create an empty map</li>
     *     <li>Otherwise, create an map with one key/value pair</li>
     * </ul>
     * @param key Key
     * @param value Value
     * @return Created parameter map
     */
    @NotNull
    public static SortedMap<String, Object> of(@NotNull String key, @Nullable Object value) {
        if (value == null) {
            return EMPTY;
        }
        SortedMap<String, Object> map = new TreeMap<>();
        map.put(key, value);
        return map;
    }
}
