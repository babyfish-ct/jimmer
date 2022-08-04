package org.babyfish.jimmer.sql.example.cache.chain;

import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheEnvironment;
import org.babyfish.jimmer.sql.cache.CacheLoader;

import java.util.*;
import java.util.function.Supplier;

class ChainCacheImpl<K, V> implements Cache<K, V> {

    private static final ThreadLocal<CacheLoader<?, ?>> LOADER_LOCAL =
        new ThreadLocal<>();

    private final Node<K, V> node;

    @SuppressWarnings("unchecked")
    public ChainCacheImpl(List<Object> operators) {
        if (operators.isEmpty()) {
            throw new IllegalArgumentException("operators cannot be empty");
        }
        Node<K, V> node = new TailNode<>();
        ListIterator<Object> itr = operators.listIterator(operators.size());
        while (itr.hasPrevious()) {
            Object operator = itr.previous();
            if (operator instanceof LoadingBinder<?, ?>) {
                node = new LoadingNode<>((LoadingBinder<K, V>) operator, node);
            } else {
                node = new SimpleNode<>((SimpleBinder<K, V>) operator, node);
            }
        }
        this.node = node;
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys, CacheEnvironment<K, V> env) {
        return usingCacheLoading(env.getLoader(), () -> node.loadAll(keys));
    }

    @Override
    public void deleteAll(Collection<K> keys) {
        node.deleteAll(keys);
    }

    private interface Node<K, V> extends CacheChain<K, V> {
        void deleteAll(Collection<K> keys);
    }

    private static class LoadingNode<K, V> implements Node<K, V> {

        private final LoadingBinder<K, V> operator;

        private final Node<K, V> next;

        private LoadingNode(LoadingBinder<K, V> operator, Node<K, V> next) {
            this.operator = operator;
            this.next = next;
            operator.initialize(next);
        }

        @Override
        public Map<K, V> loadAll(Collection<K> keys) {
            return operator.getAll(keys);
        }

        @Override
        public void deleteAll(Collection<K> keys) {
            try {
                operator.deleteAll(keys);
            } finally {
                next.deleteAll(keys);
            }
        }
    }

    private static class SimpleNode<K, V> implements Node<K, V> {

        private final SimpleBinder<K, V> operator;

        private final Node<K, V> next;

        private SimpleNode(SimpleBinder<K, V> operator, Node<K, V> next) {
            this.operator = operator;
            this.next = next;
        }

        @Override
        public Map<K, V> loadAll(Collection<K> keys) {
            Map<K, V> map = operator.getAll(keys);
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
                operator.setAll(mapFromNext);
                map.putAll(mapFromNext);
            }
            return map;
        }

        @Override
        public void deleteAll(Collection<K> keys) {
            try {
                operator.deleteAll(keys);
            } finally {
                next.deleteAll(keys);
            }
        }
    }

    private static class TailNode<K, V> implements Node<K, V> {

        @Override
        public Map<K, V> loadAll(Collection<K> keys) {
            CacheLoader<K, V> loader = currentCacheLoader();
            return loader.loadAll(keys);
        }

        @Override
        public void deleteAll(Collection<K> keys) {
        }
    }

    private static <R> R usingCacheLoading(
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
                    "Cache operator can only be called by chain cache"
            );
        }
        return (CacheLoader<K, V>) loader;
    }
}
