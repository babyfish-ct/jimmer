package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    @SafeVarargs
    static <K, V> Map<K, V> mergeMap(Map<K, V> ... maps) {
        List<Map<K, V>> nonNullMaps = Arrays
                .stream(maps)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (nonNullMaps.size() == 0) {
            return Collections.emptyMap();
        }
        if (nonNullMaps.size() == 1) {
            return nonNullMaps.get(0);
        }
        int totalCount = nonNullMaps.stream().mapToInt(Map::size).sum();
        Map<K, V> finalMap = new LinkedHashMap<>((totalCount * 4 + 2) / 3);
        for (Map<K, V> map : nonNullMaps) {
            finalMap.putAll(map);
        }
        return finalMap;
    }

    static <K, T, V> Map<K, V> joinMaps(Map<K, T> map1, Map<T, V> map2) {
        Map<K, V> map = new LinkedHashMap<>((map1.size() * 4 + 2) / 3);
        for (Map.Entry<K, T> e : map1.entrySet()) {
            V v = map2.get(e.getValue());
            if (v != null) {
                map.put(e.getKey(), v);
            }
        }
        return map;
    }

    static <K, T, V> Map<K, List<V>> joinMultiMapAndMap(
            Map<K, List<T>> map1,
            Map<T, V> map2
    ) {
        Map<K, List<V>> map = new LinkedHashMap<>((map1.size() * 4 + 2) / 3);
        for (Map.Entry<K, List<T>> e : map1.entrySet()) {
            List<T> originalValues = e.getValue();
            if (originalValues != null && !originalValues.isEmpty()) {
                List<V> values = new ArrayList<>();
                for (T t : originalValues) {
                    values.add(map2.get(t));
                }
                map.put(e.getKey(), values);
            }
        }
        return map;
    }

    static <K, T, V> Map<K, V> joinCollectionAndMap(
            Collection<K> c,
            Function<K, T> middleKeyExtractor,
            Map<T, V> map
    ) {
        Map<K, V> resultMap = new LinkedHashMap<>((c.size() * 4 + 2) / 3);
        for (K k : c) {
            T t = middleKeyExtractor.apply(k);
            V v = map.get(t);
            if (v != null) {
                resultMap.put(k, v);
            }
        }
        return resultMap;
    }

    static <K, V> Map<K, V> toMap(
            Function<V, K> keyExtractor,
            Collection<V> values
    ) {
        Map<K, V> map = new LinkedHashMap<>((values.size() * 4 + 2) / 3);
        for (V value : values) {
            K key = keyExtractor.apply(value);
            map.put(key, value);
        }
        if (map.size() < values.size()) {
            LOGGER.warn("Utils.toMap() meet duplicated keys, original collection: {}", values);
        }
        return map;
    }

    static <K, V> Map<K, List<V>> toMultiMap(
            Function<V, K> keyExtractor,
            Collection<V> values
    ) {
        return values.stream().collect(
                Collectors.groupingBy(
                        keyExtractor,
                        Collectors.toList()
                )
        );
    }

    static <K, V> List<Tuple2<K, V>> toTuples(K key, Collection<V> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<Tuple2<K, V>> tuples = new ArrayList<>(values.size());
        for (V value : values) {
            tuples.add(new Tuple2<>(key, value));
        }
        return tuples;
    }

    static <K, V> Map<K, V> toMap(K key, Collection<V> values) {
        if (values.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<K, V> map = new LinkedHashMap<>((values.size() * 4 + 2) / 3);
        for (V value : values) {
            map.put(key, value);
        }
        return map;
    }
}
