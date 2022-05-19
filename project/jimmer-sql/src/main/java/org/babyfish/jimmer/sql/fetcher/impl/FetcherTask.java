package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;

import java.sql.Connection;
import java.util.*;

class FetcherTask {

    private DataCache cache;

    private SqlClient sqlClient;

    private Connection con;

    private Field field;

    private int batchSize;

    private Map<Object, TaskData> pendingMap = new LinkedHashMap<>();

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
        add(draft, 1);
    }

    private void add(DraftSpi draft, int depth) {
        String propName = field.getProp().getName();
        Object key = cache.createKey(field, draft);
        Object value = cache.get(field, key);
        if (value != null) {
            draft.__set(propName, DataCache.unwrap(value));
            return;
        }
        pendingMap.computeIfAbsent(key, it -> new TaskData(depth)).getDrafts().add(draft);
    }

    public boolean execute() {
        if (pendingMap.isEmpty()) {
            return true;
        }
        Map<Object, TaskData> handledMap;
        if (pendingMap.size() > batchSize) {
            handledMap = new LinkedHashMap<>((batchSize * 4 + 2) / 3);
            Iterator<Map.Entry<Object, TaskData>> itr =
                    pendingMap.entrySet().iterator();
            for (int i = batchSize; i > 0; --i) {
                Map.Entry<Object, TaskData> e = itr.next();
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
            for (Map.Entry<Object, TaskData> e : handledMap.entrySet()) {
                Object key = e.getKey();
                Object value = loadedMap.get(key);
                TaskData taskData = e.getValue();
                loaded(e.getKey(), value, taskData);
            }
        }
        return pendingMap.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void loaded(Object key, Object value, TaskData taskData) {
        cache.put(field, key, value);
        for (DraftSpi draft : taskData.getDrafts()) {
            if (value == null && field.getProp().isEntityList()) {
                draft.__set(field.getProp().getName(), Collections.emptyList());
            } else {
                draft.__set(field.getProp().getName(), value);
            }
        }
        RecursionStrategy<Object> recursionStrategy =
                (RecursionStrategy<Object>) field.getRecursionStrategy();
        if (value instanceof List<?>) {
            List<ImmutableSpi> targets = (List<ImmutableSpi>) value;
            for (ImmutableSpi target : targets) {
                DraftContext draftContext = Internal.currentDraftContext();
                if (recursionStrategy != null && recursionStrategy.isFetchable(target, taskData.depth)) {
                    add(draftContext.toDraftObject(target), taskData.getDepth() + 1);
                }
            }
        } else if (value != null &&
                recursionStrategy != null &&
                recursionStrategy.isFetchable(value, taskData.depth)) {
            DraftContext draftContext = Internal.currentDraftContext();
            add(draftContext.toDraftObject(value), taskData.getDepth() + 1);
        }
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

    private static class TaskData {

        private int depth;

        private Collection<DraftSpi> drafts = new ArrayList<>();

        public TaskData(int depth) {
            this.depth = depth;
        }

        public int getDepth() {
            return depth;
        }

        public Collection<DraftSpi> getDrafts() {
            return drafts;
        }

        @Override
        public String toString() {
            return "TaskData{" +
                    "depth=" + depth +
                    ", drafts=" + drafts +
                    '}';
        }
    }
}
