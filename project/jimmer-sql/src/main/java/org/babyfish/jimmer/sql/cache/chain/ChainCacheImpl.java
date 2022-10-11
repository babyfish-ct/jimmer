package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheEnvironment;
import org.babyfish.jimmer.sql.cache.CacheLoader;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

class ChainCacheImpl<K, V> implements Cache<K, V> {

    private static final ThreadLocal<CacheLoader<?, ?>> LOADER_LOCAL =
        new ThreadLocal<>();

    private final Node<K, V> node;

    @SuppressWarnings("unchecked")
    public ChainCacheImpl(List<Object> binders) {
        if (binders.isEmpty()) {
            throw new IllegalArgumentException("binders cannot be empty");
        }
        Node<K, V> node = new TailNode<>();
        ListIterator<Object> itr = binders.listIterator(binders.size());
        while (itr.hasPrevious()) {
            Object binder = itr.previous();
            node = createNode(binder, node);
        }
        this.node = node;
    }

    @NotNull
    @Override
    public Map<K, V> getAll(@NotNull Collection<K> keys, @NotNull CacheEnvironment<K, V> env) {
        return usingCacheLoading(env.getLoader(), () -> node.loadAll(keys));
    }

    @Override
    public void deleteAll(@NotNull Collection<K> keys, Object reason) {
        node.deleteAll(keys, reason);
    }

    @SuppressWarnings("unchecked")
    protected Node<K, V> createNode(Object binder, Node<K, V> next) {
        if (binder instanceof LoadingBinder<?, ?>) {
            return new LoadingNode<>((LoadingBinder<K, V>) binder, next);
        }
        return new SimpleNode<>((SimpleBinder<K, V>) binder, next);
    }

    protected interface Node<K, V> extends CacheChain<K, V> {
        void deleteAll(@NotNull Collection<K> keys, Object reason);
    }

    private static class LoadingNode<K, V> implements Node<K, V> {

        private final LoadingBinder<K, V> binder;

        private final Node<K, V> next;

        LoadingNode(LoadingBinder<K, V> binder, Node<K, V> next) {
            this.binder = binder;
            this.next = next;
            binder.initialize(next);
        }

        @NotNull
        @Override
        public Map<K, V> loadAll(@NotNull Collection<K> keys) {
            return binder.getAll(keys);
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, Object reason) {
            next.deleteAll(keys, reason);
            binder.deleteAll(keys, reason);
        }
    }

    protected static class SimpleNode<K, V> implements Node<K, V> {

        protected final SimpleBinder<K, V> binder;

        protected final Node<K, V> next;

        protected SimpleNode(SimpleBinder<K, V> binder, Node<K, V> next) {
            this.binder = binder;
            this.next = next;
        }

        @NotNull
        @Override
        public Map<K, V> loadAll(@NotNull Collection<K> keys) {
            Map<K, V> map = binder.getAll(keys);
            if (map.size() < keys.size()) {
                Set<K> missedKeys = new LinkedHashSet<>();
                for (K key : keys) {
                    if (!map.containsKey(key)) {
                        missedKeys.add(key);
                    }
                }
                Map<K, V> mapFromNext = next.loadAll(missedKeys);
                if (mapFromNext.size() < missedKeys.size()) {
                    for (K missedKey : missedKeys) {
                        if (!mapFromNext.containsKey(missedKey)) {
                            mapFromNext.put(missedKey, null);
                        }
                    }
                }
                binder.setAll(mapFromNext);
                map.putAll(mapFromNext);
            }
            return map;
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, Object reason) {
            next.deleteAll(keys, reason);
            binder.deleteAll(keys, reason);
        }
    }

    protected static class TailNode<K, V> implements Node<K, V> {

        @NotNull
        @Override
        public Map<K, V> loadAll(@NotNull Collection<K> keys) {
            CacheLoader<K, V> loader = currentCacheLoader();
            return loader.loadAll(keys);
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, Object reason) {
        }
    }

    protected static <R> R usingCacheLoading(
            CacheLoader<?, ?> loader,
            Supplier<R> block
    ) {
        if (loader == null) {
            throw new IllegalArgumentException("loader cannot be null");
        }
        CacheLoader<?, ?> oldLoader = LOADER_LOCAL.get();
        LOADER_LOCAL.set(loader);
        try {
            return block.get();
        } finally {
            if (oldLoader != null) {
                LOADER_LOCAL.set(oldLoader);
            } else {
                LOADER_LOCAL.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> CacheLoader<K, V> currentCacheLoader() {
        CacheLoader<?, ?> loader = LOADER_LOCAL.get();
        if (loader == null) {
            throw new IllegalStateException(
                    "Cache binder can only be called by chain cache"
            );
        }
        return (CacheLoader<K, V>) loader;
    }
}
