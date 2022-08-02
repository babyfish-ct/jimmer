package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.runtime.Internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class CacheLoaderWrapper<K, V> implements CacheLoader<K, V> {

    private final CacheLoader<K, V> raw;

    private final boolean requiresNewDraftContext;

    CacheLoaderWrapper(CacheLoader<K, V> raw, boolean requiresNewDraftContext) {
        this.raw = raw;
        this.requiresNewDraftContext = requiresNewDraftContext;
    }

    static <K, V> CacheLoader<K, V> wrap(CacheLoader<K, V> loader, boolean requiresNewDraftContext) {
        if (loader instanceof CacheLoaderWrapper<?, ?>) {
            return loader;
        }
        return new CacheLoaderWrapper<>(loader, requiresNewDraftContext);
    }

    @Override
    public Map<K, V> loadAll(Collection<K> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }
        if (!requiresNewDraftContext) {
            return raw.loadAll(keys);
        }
        return Internal.requiresNewDraftContext(ctx -> {
            Map<K, V> map = raw.loadAll(keys);
            Map<K, V> resolvedMap = new HashMap<>((map.size() * 4 + 2) / 3);
            for (Map.Entry<K, V> e : map.entrySet()) {
                resolvedMap.put(e.getKey(), ctx.resolveObject(e.getValue()));
            }
            return resolvedMap;
        });
    }
}
