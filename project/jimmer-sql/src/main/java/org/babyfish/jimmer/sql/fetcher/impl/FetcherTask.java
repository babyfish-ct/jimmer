package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

class FetcherTask {

    private final FetchingCache cache;

    private final JSqlClient sqlClient;

    private final Field field;

    private final int batchSize;

    private final DataLoader dataLoader;

    private Map<Object, TaskData> pendingMap = new LinkedHashMap<>();

    public FetcherTask(
            FetchingCache cache,
            JSqlClient sqlClient,
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
        Object key = cache.createKey(field, draft);
        if (key == null) {
            return;
        }
        Object value = cache.get(field, key);
        if (value != null) {
            setDraftProp(draft, FetchingCache.unwrap(value));
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
                value = FetchingCache.unwrap(value);
                TaskData taskData = e.getValue();
                afterLoad(taskData, value,false);
                handledEntryItr.remove();
            }
        }
        if (!handledMap.isEmpty()) {
            Map<ImmutableSpi, ?> loadedMap = dataLoader.load(
                    handledMap
                            .values()
                            .stream()
                            .map(it -> it.getDrafts().get(0))
                            .collect(Collectors.toList())
            );
            for (Map.Entry<Object, TaskData> e : handledMap.entrySet()) {
                TaskData taskData = e.getValue();
                Object value = loadedMap.get(taskData.getDrafts().get(0));
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
        Object childValue = draft.__get(field.getProp().getId());
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
        return ((DraftSpi) obj).__isLoaded(field.getProp().getId());
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
            if (field.getProp().isReferenceList(TargetLevel.ENTITY)) {
                return sqlClient.getDefaultListBatchSize();
            }
            return sqlClient.getDefaultBatchSize();
        }
        return size;
    }

    private void setDraftProp(DraftSpi draft, Object value) {
        if (value == null && field.getProp().isReferenceList(TargetLevel.ENTITY)) {
            draft.__set(field.getProp().getId(), Collections.emptyList());
        } else {
            draft.__set(field.getProp().getId(), value);
        }
    }

    private static class TaskData {

        private final Object key;

        private final int depth;

        private final List<DraftSpi> drafts = new ArrayList<>();

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
        public String toString() {
            return "TaskData{" +
                    "depth=" + depth +
                    ", drafts=" + drafts +
                    '}';
        }
    }
}