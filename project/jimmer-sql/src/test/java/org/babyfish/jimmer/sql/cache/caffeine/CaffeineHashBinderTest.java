package org.babyfish.jimmer.sql.cache.caffeine;

import org.babyfish.jimmer.sql.model.BookProps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;

public class CaffeineHashBinderTest {

    @Test
    public void testParameterizedValues() {
        CaffeineHashBinder<Integer, String> binder =
                new CaffeineHashBinder<>(null, BookProps.STORE.unwrap(), null, 100, Duration.ofMinutes(1));
        SortedMap<String, Object> parameterMap1 = parameterMap("a");
        SortedMap<String, Object> parameterMap2 = parameterMap("b");

        binder.setAll(Collections.singletonMap(1, "value"), parameterMap1);
        Assertions.assertFalse(
                binder.getAll(Collections.singleton(1), parameterMap2).containsKey(1)
        );

        Map<Integer, String> nullValueMap = new HashMap<>();
        nullValueMap.put(1, null);
        binder.setAll(nullValueMap, parameterMap2);

        Map<Integer, String> resultMap1 = binder.getAll(Collections.singleton(1), parameterMap1);
        Assertions.assertEquals("value", resultMap1.get(1));
        Map<Integer, String> resultMap2 = binder.getAll(Collections.singleton(1), parameterMap2);
        Assertions.assertTrue(resultMap2.containsKey(1));
        Assertions.assertNull(resultMap2.get(1));
    }

    private static SortedMap<String, Object> parameterMap(String value) {
        SortedMap<String, Object> map = new TreeMap<>();
        map.put("key", value);
        return map;
    }
}
