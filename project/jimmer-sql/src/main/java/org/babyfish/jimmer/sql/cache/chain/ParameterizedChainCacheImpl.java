package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheEnvironment;
import org.babyfish.jimmer.sql.cache.CacheLoader;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class ParameterizedChainCacheImpl<K, V> extends ChainCacheImpl<K, V> implements Cache.Parameterized<K, V> {

    public ParameterizedChainCacheImpl(List<Object> binders) {
        super(binders);
        boolean hasParameterizedBinder = false;
        for (Object binder : binders) {
            boolean isParameterizedBinder =
                    binder instanceof LoadingBinder.Parameterized<?, ?> ||
                            binder instanceof SimpleBinder.Parameterized<?, ?>;
            if (hasParameterizedBinder && !isParameterizedBinder) {
                throw new IllegalArgumentException(
                        "Parameterized binder must be after other binders"
                );
            }
            hasParameterizedBinder = isParameterizedBinder;
        }
        if (!hasParameterizedBinder) {
            throw new IllegalArgumentException("No parameterized binders");
        }
    }

    @Override
    @NotNull
    public Map<K, V> getAll(
            @NotNull Collection<K> keys,
            @NotNull NavigableMap<String, Object> parameterMap,
            @NotNull CacheEnvironment<K, V> env
    ) {
        return usingCacheLoading(
                env.getLoader(), () -> ((ParameterizedNode<K, V>)node).loadAll(keys, parameterMap)
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Node<K, V> createNode(Object binder, Node<K, V> next) {
        if (binder instanceof LoadingBinder.Parameterized<?, ?>) {
            return new ParameterizedLoadingNode<K, V>(
                    (LoadingBinder.Parameterized<K, V>) binder,
                    (ParameterizedNode<K, V>) next
            );
        }
        if (binder instanceof SimpleBinder.Parameterized<?, ?>) {
            return new ParameterizedSimpleNode<K, V>(
                    (SimpleBinder.Parameterized<K, V>) binder,
                    next
            );
        }
        return super.createNode(binder, next);
    }

    @Override
    protected TailNode<K, V> createTailNode() {
        return new TailNode<>();
    }

    protected interface ParameterizedNode<K, V> extends Node<K, V>, CacheChain.Parameterized<K, V> {}

    private static class TailNode<K, V> extends ChainCacheImpl.TailNode<K, V> implements ParameterizedNode<K, V> {

        @Override
        public @NotNull Map<K, V> loadAll(@NotNull Collection<K> keys, @NotNull NavigableMap<String, Object> parameterMap) {
            CacheLoader<K, V> loader = currentCacheLoader();
            return loader.loadAll(keys);
        }
    }

    private static class ParameterizedLoadingNode<K, V> implements ParameterizedNode<K, V> {

        private final LoadingBinder.Parameterized<K, V> binder;

        private final ParameterizedNode<K, V> next;

        ParameterizedLoadingNode(LoadingBinder.Parameterized<K, V> binder, ParameterizedNode<K, V> next) {
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
        @NotNull
        public Map<K, V> loadAll(
                @NotNull Collection<K> keys,
                @NotNull NavigableMap<String, Object> parameterMap
        ) {
            return binder.getAll(keys, parameterMap);
        }

        @Override
        public void deleteAll(@NotNull Collection<K> keys, Object reason) {
            next.deleteAll(keys, reason);
            binder.deleteAll(keys, reason);
        }
    }

    protected static class ParameterizedSimpleNode<K, V> extends SimpleNode<K, V> implements ParameterizedNode<K, V> {

        protected ParameterizedSimpleNode(SimpleBinder.Parameterized<K, V> binder, Node<K, V> next) {
            super(binder, next);
        }

        @Override
        public @NotNull Map<K, V> loadAll(
                @NotNull Collection<K> keys,
                @NotNull NavigableMap<String, Object> parameterMap
        ) {
            SimpleBinder.Parameterized<K, V> parameterizedBinder =
                    (SimpleBinder.Parameterized<K, V>) binder;
            Map<K, V> map = parameterizedBinder.getAll(keys, parameterMap);
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
                parameterizedBinder.setAll(mapFromNext, parameterMap);
                map.putAll(mapFromNext);
            }
            return map;
        }
    }
}
