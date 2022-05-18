package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.fetcher.Field;

import java.sql.Connection;
import java.util.*;

class FetcherTask {

    private DataCache cache;

    private SqlClient sqlClient;

    private Connection con;

    private Field field;

    private int batchSize;

    private Map<Object, Collection<DraftSpi>> pendingMap = new LinkedHashMap<>();

    private BatchDataLoader batchDataLoader;

    public FetcherTask(
            DataCache cache,
            SqlClient sqlClient,
            Connection con,
            Field field
    ) {
        this.cache = cache;
        this.sqlClient = sqlClient;
        this.con = con;
        this.field = field;
        this.batchSize = determineBatchSize();
        this.batchDataLoader = new BatchDataLoader(sqlClient, con, field);
    }

    public void add(DraftSpi draft) {
        String propName = field.getProp().getName();
        Object key = cache.createKey(field, draft);
        Object value = cache.get(field, key);
        if (value != null) {
            draft.__set(propName, DataCache.unwrap(value));
            return;
        }
        pendingMap.computeIfAbsent(key, it -> new ArrayList<>()).add(draft);
    }

    public boolean execute() {
        if (pendingMap.isEmpty()) {
            return true;
        }
        Map<Object, Collection<DraftSpi>> handledMap;
        if (pendingMap.size() > batchSize) {
            handledMap = new LinkedHashMap<>((batchSize * 4 + 2) / 3);
            Iterator<Map.Entry<Object, Collection<DraftSpi>>> itr =
                    pendingMap.entrySet().iterator();
            for (int i = batchSize; i > 0; --i) {
                Map.Entry<Object, Collection<DraftSpi>> e = itr.next();
                handledMap.put(e.getKey(), e.getValue());
                itr.remove();
            }
        } else {
            handledMap = this.pendingMap;
            pendingMap = new LinkedHashMap<>();
        }
        if (batchSize == 1) {
            throw new UnsupportedOperationException();
        } else {
            Map<Object, ?> loadedMap = batchDataLoader.load(handledMap.keySet());
            String propName = field.getProp().getName();
            for (Map.Entry<Object, Collection<DraftSpi>> e : handledMap.entrySet()) {
                Object key = e.getKey();
                Object value = loadedMap.get(key);
                cache.put(field, key, value);
                for (DraftSpi draft : e.getValue()) {
                    draft.__set(propName, value);
                }
            }
        }
        return pendingMap.isEmpty();
    }

    private int determineBatchSize() {
        if (field.getLimit() != Integer.MAX_VALUE) {
            return 1;
        }
        int size = field.getBatchSize();
        if (size == 0) {
            if (field.getProp().isEntityList()) {
                return sqlClient.getDefaultListBatchSize();
            }
            return sqlClient.getDefaultBatchSize();
        }
        return size;
    }
}
