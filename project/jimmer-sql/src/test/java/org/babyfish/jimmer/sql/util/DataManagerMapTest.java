package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class DataManagerMapTest {

    private FastMap map;

    @BeforeEach
    public void initialize() {
        map = new FastMap();
    }

    @Test
    public void test() {

        for (int i = 0; i < 10; i++) {
            String key = "key" + i;
            String value = "value" + i;
            Assertions.assertNull(map.get(key));
            map.put(key, value);
            Assertions.assertEquals(value, map.get(key));
        }
        Assertions.assertEquals(
                "[value0, value1, value2, value3, value4, value5, value6, value7, value8, value9]",
                map.toList().toString()
        );

        for (int i = 0; i < 10; i++) {
            String key = "key" + i;
            String value = map.get(key);
            value = "new(" + value + ")";
            map.put(key, value);
            Assertions.assertEquals(value, map.get(key));
        }
        Assertions.assertEquals(
                "[" +
                        "new(value0), new(value1), new(value2), new(value3), new(value4), " +
                        "new(value5), new(value6), new(value7), new(value8), new(value9)" +
                        "]",
                map.toList().toString()
        );
    }

    private static class FastMap extends AbstractDataManager<String, String> {

        public String get(String key) {
            return super.getValue(key);
        }

        public void put(String key, String value) {
            super.putValue(key, value);
        }

        public List<String> toList() {
            List<String> list = new ArrayList<>();
            for (String value : this) {
                list.add(value);
            }
            return list;
        }
    }
}
