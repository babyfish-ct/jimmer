package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

public class CacheTest {

    private List<History> histories;

    private Impl impl1;

    private Impl impl2;

    private Cache<Integer, String> cache;

    @BeforeEach
    public void initialize() {
        histories = new ArrayList<>();
        impl1 = new Impl(
                "impl-1",
                false,
                Arrays.asList(
                        new Tuple2<>("memory-1", "One"),
                        new Tuple2<>("memory-1:{mode=odd}", "One"),
                        new Tuple2<>("memory-1:{mode=even}", null)
                ),
                histories
        );
        impl2 = new Impl(
                "impl-2",
                true,
                Arrays.asList(
                        new Tuple2<>("memory-1", "One"),
                        new Tuple2<>("memory-1:{mode=odd}", "One"),
                        new Tuple2<>("memory-1:{mode=even}", null),
                        new Tuple2<>("memory-2", "Two"),
                        new Tuple2<>("memory-2:{mode=odd}", null),
                        new Tuple2<>("memory-2:{mode=even}", "Two")
                ),
                histories
        );
        cache = new CacheImpl<>(
                "memory-",
                CacheBinder.of(impl1, impl2),
                new Loader(
                        histories,
                        Arrays.asList(
                                new Tuple2<>(1, "One"),
                                new Tuple2<>(2, "Two"),
                                new Tuple2<>(3, "Three"),
                                new Tuple2<>(4, "Four")
                        )
                ),
                new Locker(histories)
        );
    }

    @Test
    public void test() {

        Assertions.assertEquals(
                "{3=Three, 1=One, 4=Four, 2=Two}",
                cache.getAll(Arrays.asList(3, 1, 4, 2, 3, 4, 2, 1, 7, 8)).toString()
        );
        Assertions.assertEquals(
                "[" +
                        "History{action='lockAll', data=[memory-3, memory-4, memory-7, memory-8]}, " +
                        "History{action='loadAll', data=[[3, 4, 7, 8], null]}, " +
                        "History{action='setAll:impl-1', data={memory-3=Three, memory-4=Four, memory-2=Two}}, " +
                        "History{action='setAll:impl-2', data={memory-3=Three, memory-4=Four, memory-7=null, memory-8=null}}, " +
                        "History{action='unlockAll', data=[memory-3, memory-4, memory-7, memory-8]}" +
                        "]",
                histories.toString()
        );
    }

    @Test
    public void testOdd() {

        Assertions.assertEquals(
                "{3=Three, 1=One}",
                cache.getAll(
                        Arrays.asList(3, 1, 4, 2, 3, 4, 2, 1, 7, 8),
                        CacheFilter.of(new Tuple2<>("mode", "odd"))
                ).toString()
        );
        Assertions.assertEquals(
                (
                        "[" +
                                "--->History{" +
                                "--->--->action='lockAll', " +
                                "--->--->data=[" +
                                "--->--->--->memory-3:{mode=odd}, " +
                                "--->--->--->memory-4:{mode=odd}, " +
                                "--->--->--->memory-7:{mode=odd}, " +
                                "--->--->--->memory-8:{mode=odd}" +
                                "--->--->]" +
                                "--->}, " +
                                "--->History{" +
                                "--->--->action='loadAll', " +
                                "--->--->data=[" +
                                "--->--->--->[3, 4, 7, 8], " +
                                "--->--->--->CacheFilterImpl{args={mode=odd}}" +
                                "--->--->]" +
                                "--->}, " +
                                "--->History{" +
                                "--->--->action='setAll:impl-1', " +
                                "--->--->data={memory-3:{mode=odd}=Three}" +
                                "--->}, " +
                                "--->History{" +
                                "--->--->action='setAll:impl-2', " +
                                "--->--->data={" +
                                "--->--->--->memory-3:{mode=odd}=Three, " +
                                "--->--->--->memory-4:{mode=odd}=null, " +
                                "--->--->--->memory-7:{mode=odd}=null, " +
                                "--->--->--->memory-8:{mode=odd}=null" +
                                "--->--->}" +
                                "--->}, " +
                                "--->History{" +
                                "--->--->action='unlockAll', " +
                                "--->--->data=[" +
                                "--->--->--->memory-3:{mode=odd}, " +
                                "--->--->--->memory-4:{mode=odd}, " +
                                "--->--->--->memory-7:{mode=odd}, " +
                                "--->--->--->memory-8:{mode=odd}" +
                                "--->--->]" +
                                "--->}" +
                                "]"
                ).replace("--->", ""),
                histories.toString()
        );
    }

    @Test
    public void testEven() {

        Assertions.assertEquals(
                "{4=Four, 2=Two}",
                cache.getAll(
                        Arrays.asList(3, 1, 4, 2, 3, 4, 2, 1, 7, 8),
                        CacheFilter.of(new Tuple2<>("mode", "even"))
                ).toString()
        );
        Assertions.assertEquals(
                (
                        "[" +
                                "--->History{" +
                                "--->--->action='lockAll', " +
                                "--->--->data=[" +
                                "--->--->--->memory-3:{mode=even}, " +
                                "--->--->--->memory-4:{mode=even}, " +
                                "--->--->--->memory-7:{mode=even}, " +
                                "--->--->--->memory-8:{mode=even}" +
                                "--->--->]" +
                                "--->}, " +
                                "--->History{" +
                                "--->--->action='loadAll', " +
                                "--->--->data=[" +
                                "--->--->--->[3, 4, 7, 8], " +
                                "--->--->--->CacheFilterImpl{args={mode=even}}" +
                                "--->--->]" +
                                "--->}, " +
                                "--->History{" +
                                "--->--->action='setAll:impl-1', " +
                                "--->--->data={memory-4:{mode=even}=Four, memory-2:{mode=even}=Two}" +
                                "--->}, " +
                                "--->History{" +
                                "--->--->action='setAll:impl-2', " +
                                "--->--->data={" +
                                "--->--->--->memory-3:{mode=even}=null, " +
                                "--->--->--->memory-4:{mode=even}=Four, " +
                                "--->--->--->memory-7:{mode=even}=null, " +
                                "--->--->--->memory-8:{mode=even}=null" +
                                "--->--->}" +
                                "--->}, " +
                                "--->History{" +
                                "--->--->action='unlockAll', " +
                                "--->--->data=[" +
                                "--->--->--->memory-3:{mode=even}, " +
                                "--->--->--->memory-4:{mode=even}, " +
                                "--->--->--->memory-7:{mode=even}, " +
                                "--->--->--->memory-8:{mode=even}" +
                                "--->--->]" +
                                "--->}" +
                                "]"
                ).replace("--->", ""),
                histories.toString()
        );
    }

    private static class Impl implements CacheImplementation<String> {

        private String name;

        private final boolean isNullSavable;

        private final Map<String, String> valueMap;

        private List<History> histories;

        Impl(
                String name,
                boolean isNullSavable,
                List<Tuple2<String, String>> tuples,
                List<History> histories
        ) {
            this.name = name;
            Map<String, String> valueMap = new LinkedHashMap<>();
            for (Tuple2<String, String> tuple : tuples) {
                valueMap.put(tuple._1(), tuple._2());
            }
            this.isNullSavable = isNullSavable;
            this.valueMap = valueMap;
            this.histories = histories;
        }

        @Override
        public boolean isNullSavable() {
            return isNullSavable;
        }

        @Override
        public Map<String, String> getAll(Set<String> keys) {
            Map<String, String> cachedMap = new LinkedHashMap<>();
            for (String key : keys) {
                String value = valueMap.get(key);
                if (value != null || valueMap.containsKey(key)) {
                    cachedMap.put(key, value);
                }
            }
            return cachedMap;
        }

        @Override
        public void setAll(Map<String, String> map) {
            histories.add(
                    new History(
                            "setAll:" + name,
                            map
                    )
            );
            valueMap.putAll(map);
        }

        @Override
        public void deleteAll(Set<String> keys) {
            valueMap.keySet().removeAll(keys);
        }
    }

    private static class Loader implements CacheLoader<Integer, String> {

        private final List<History> histories;

        private final Map<Integer, String> valueMap;

        Loader(List<History> histories, List<Tuple2<Integer, String>> tuples) {
            Map<Integer, String> valueMap = new LinkedHashMap<>();
            for (Tuple2<Integer, String> tuple : tuples) {
                valueMap.put(tuple._1(), tuple._2());
            }
            this.histories = histories;
            this.valueMap = valueMap;
        }

        @Override
        public Map<Integer, String> loadAll(Set<Integer> keys, CacheFilter filter) {
            histories.add(
                    new History(
                            "loadAll",
                            Arrays.asList(keys, filter)
                    )
            );
            String mode = null;
            if (filter != null) {
                mode = (String)filter.toCacheArgs().get("mode");
            }
            Map<Integer, String> cachedMap = new LinkedHashMap<>();
            for (Integer key : keys) {
                if ("odd".equals(mode) && key % 2 == 0) {
                    continue;
                }
                if ("even".equals(mode) && key % 2 != 0) {
                    continue;
                }
                cachedMap.put(key, valueMap.get(key));
            }
            return cachedMap;
        }
    }

    private static class Locker implements CacheLocker {

        private final List<History> histories;

        private Locker(List<History> histories) {
            this.histories = histories;
        }

        @Override
        public void lockAll(Set<String> keys) {
            histories.add(new History("lockAll", keys));
        }

        @Override
        public void unlockAll(Set<String> keys) {
            histories.add(new History("unlockAll", keys));
        }
    }

    private static class History {

        private final String action;

        private final Object data;

        private History(String action, Object data) {
            this.action = action;
            this.data = data;
        }

        @Override
        public int hashCode() {
            return Objects.hash(action, data);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            History history = (History) o;
            return action.equals(history.action) && data.equals(history.data);
        }

        @Override
        public String toString() {
            return "History{" +
                    "action='" + action + '\'' +
                    ", data=" + data +
                    '}';
        }
    }
}
