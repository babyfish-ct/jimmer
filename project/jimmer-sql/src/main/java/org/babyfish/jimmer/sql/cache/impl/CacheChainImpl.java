package org.babyfish.jimmer.sql.cache.impl;

import org.babyfish.jimmer.sql.cache.CacheChain;

import java.util.Map;
import java.util.Set;

class CacheChainImpl<V> implements CacheChain<V> {

    private final OperationContext<?, V> ctx;

    private final int cursor;

    public CacheChainImpl(OperationContext<?, V> ctx, int cursor) {
        this.ctx = ctx;
        this.cursor = cursor;
    }

    @Override
    public Map<String, V> getAll(Set<String> storageKeys) {
        if (cursor >= ctx.size()) {
            return ctx.loadAll(storageKeys);
        }
        return ctx
                .provider(cursor)
                .getAll(
                        storageKeys,
                        new CacheChainImpl<>(ctx, cursor + 1)
                );
    }
}
