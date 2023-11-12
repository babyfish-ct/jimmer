package org.babyfish.jimmer.sql.example.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CacheStorage {

    public final ConcurrentMap<String, Object> objectMap =
            new ConcurrentHashMap<>();

    public final ConcurrentMap<String, Object> singleViewPropMap =
            new ConcurrentHashMap<>();

    public final ConcurrentMap<String, ConcurrentMap<String, Object>> multiViewPropCache =
            new ConcurrentHashMap<>();

    public void trace() {

        System.out.println("---- Object Cache Items ----");
        for (Map.Entry<String, Object> e : objectMap.entrySet()) {
            System.out.println(e.getKey() + ": " + e.getValue());
        }

        System.out.println("---- Single-view Prop Cache Items ----");
        for (Map.Entry<String, Object> e : singleViewPropMap.entrySet()) {
            System.out.println(e.getKey() + ": " + e.getValue());
        }

        System.out.println("---- Multi-view Prop Cache Items ----");
        for (Map.Entry<String, ConcurrentMap<String, Object>> e : multiViewPropCache.entrySet()) {
            System.out.println(e.getKey() + ':');
            for (Map.Entry<String, Object> subE : e.getValue().entrySet()) {
                System.out.println(subE.getKey() + ": " + subE.getValue());
            }
        }
    }

    public void clear() {
        objectMap.clear();
        singleViewPropMap.clear();
        multiViewPropCache.clear();
    }
}
