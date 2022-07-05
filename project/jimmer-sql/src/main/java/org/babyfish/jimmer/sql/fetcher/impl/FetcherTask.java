package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;
import org.babyfish.jimmer.sql.meta.Column;

import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;

class FetcherTask {

    private final DataCache cache;

    private final SqlClient sqlClient;

    private Field field;

    private final int batchSize;

    private final DataLoader dataLoader;

    private Map<Object, TaskData> pendingMap = new LinkedHashMap<>();

    public FetcherTask(
            DataCache cache,
            SqlClient sqlClient,
            Connection con,
            Field field
    ) {
        this.cache = cache;
        this.sqlClient = sqlClient;
        this.field = field;
        this.batchSize = determineBatchSize();
        this.dataLoader = new DataLoader(sqlClient, con, field);
    }

    public void add(DraftSpi draft) {
        add(draft, 1);
    }

    private void add(DraftSpi draft, int depth) {
        if (isLoaded(draft)) {
            return;
        }
        Object key = DataCache.createKey(field, draft);
        Object value = cache.get(field, key);
        if (value != null) {
            setDraftProp(draft, DataCache.unwrap(value));
            return;
        }
        pendingMap.computeIfAbsent(key, it -> new TaskData(key, depth)).getDrafts().add(draft);
    }

    public boolean execute() {
        if (pendingMap.isEmpty()) {
            return true;
        }
        Map<Object, TaskData> handledMap;
        if (pendingMap.size() > batchSize) {
            Iterator<Map.Entry<Object, TaskData>> itr =
                    pendingMap.entrySet().iterator();
            if (batchSize == 1) {
                Map.Entry<Object, TaskData> e = itr.next();
                handledMap = Collections.singletonMap(e.getKey(), e.getValue());
                itr.remove();
            } else {
                handledMap = new LinkedHashMap<>((batchSize * 4 + 2) / 3);
                for (int i = batchSize; i > 0; --i) {
                    Map.Entry<Object, TaskData> e = itr.next();
                    handledMap.put(e.getKey(), e.getValue());
                    itr.remove();
                }
            }
        } else {
            handledMap = this.pendingMap;
            pendingMap = new LinkedHashMap<>();
        }
        Iterator<Map.Entry<Object, TaskData>> handledEntryItr =
                handledMap.entrySet().iterator();
        while (handledEntryItr.hasNext()) {
            Map.Entry<Object, TaskData> e = handledEntryItr.next();
            Object key = e.getKey();
            Object value = cache.get(field, key);
            if (value != null) {
                value = DataCache.unwrap(value);
                TaskData taskData = e.getValue();
                afterLoad(taskData, value, false);
                handledEntryItr.remove();
            }
        }
        if (!handledMap.isEmpty()) {
            Map<TaskData, ImmutableSpi> sourceMap =
                    new LinkedHashMap<>((handledMap.size() * 4 + 2) / 3);
            for (TaskData taskData : handledMap.values()) {
                sourceMap.put(taskData, taskData.getDrafts().get(0));
            }
            Map<ImmutableSpi, Object> loadedMap = dataLoader.load(sourceMap.values());
            for (Map.Entry<TaskData, ImmutableSpi> e : sourceMap.entrySet()) {
                TaskData taskData = e.getKey();
                Object value = loadedMap.get(e.getValue());
                afterLoad(taskData, value, true);
            }
        }
        return pendingMap.isEmpty();
    }

    private boolean isLoaded(DraftSpi draft) {
        if (!isLoaded(draft, field)) {
            return false;
        }
        Fetcher<?> childFetcher = field.getChildFetcher();
        Object childValue = draft.__get(field.getProp().getName());
        if (childFetcher != null && childValue != null) {
           for (Field childField : childFetcher.getFieldMap().values()) {
               if (!isLoaded(childValue, childField)) {
                   return false;
               }
           }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean isLoaded(Object obj, Field field) {
        if (obj instanceof List<?>) {
            List<DraftSpi> drafts = (List<DraftSpi>) obj;
            for (DraftSpi draft : drafts) {
                if (!isLoaded(draft, field)) {
                    return false;
                }
            }
            return true;
        }
        return ((DraftSpi) obj).__isLoaded(field.getProp().getName());
    }

    @SuppressWarnings("unchecked")
    private void afterLoad(TaskData taskData, Object value, boolean updateCache) {
        if (updateCache) {
            cache.put(field, taskData.getKey(), value);
        }
        for (DraftSpi draft : taskData.getDrafts()) {
            setDraftProp(draft, value);
        }
        RecursionStrategy<Object> recursionStrategy =
                (RecursionStrategy<Object>) field.getRecursionStrategy();
        if (value instanceof List<?>) {
            List<ImmutableSpi> targets = (List<ImmutableSpi>) value;
            for (ImmutableSpi target : targets) {
                DraftContext draftContext = Internal.currentDraftContext();
                if (recursionStrategy != null &&
                        recursionStrategy.isRecursive(
                                new RecursionStrategy.Args<>(target, taskData.depth)
                        )
                ) {
                    add(draftContext.toDraftObject(target), taskData.getDepth() + 1);
                }
            }
        } else if (value != null &&
                recursionStrategy != null &&
                recursionStrategy.isRecursive(
                        new RecursionStrategy.Args<>(value, taskData.depth)
                )
        ) {
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

    private void setDraftProp(DraftSpi draft, Object value) {
        if (value == null && field.getProp().isEntityList()) {
            draft.__set(field.getProp().getName(), Collections.emptyList());
        } else {
            draft.__set(field.getProp().getName(), value);
        }
    }

    private class TaskData {

        private Object key;

        private int depth;

        private List<DraftSpi> drafts = new ArrayList<>();

        public TaskData(Object key, int depth) {
            this.key = key;
            this.depth = depth;
        }

        public Object getKey() {
            return key;
        }

        public int getDepth() {
            return depth;
        }

        public List<DraftSpi> getDrafts() {
            return drafts;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TaskData taskData = (TaskData) o;
            return Objects.equals(key, taskData.key);
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
