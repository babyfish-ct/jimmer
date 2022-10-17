package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.sql.cache.Cache;

import java.util.ArrayList;
import java.util.List;

public class ChainCacheBuilder<K, V> {

    private final List<Object> binders = new ArrayList<>();

    private Boolean hasParameterizedBinder = null;

    public ChainCacheBuilder<K, V> add(LoadingBinder<K, V> binder) {
        if (binder != null) {
            if (Boolean.TRUE.equals(hasParameterizedBinder)) {
                throw new IllegalArgumentException(
                        "Parameterized binder and normal binder cannot be mixed"
                );
            }
            hasParameterizedBinder = false;
            binders.add(binder);
        }
        return this;
    }

    public ChainCacheBuilder<K, V> add(LoadingBinder.Parameterized<K, V> binder) {
        if (binder != null) {
            if (Boolean.FALSE.equals(hasParameterizedBinder)) {
                throw new IllegalArgumentException(
                        "Parameterized binder and normal binder cannot be mixed"
                );
            }
            hasParameterizedBinder = true;
            binders.add(binder);
        }
        return this;
    }

    public ChainCacheBuilder<K, V> add(SimpleBinder<K, V> binder) {
        if (binder != null) {
            boolean isParameterized = binder instanceof SimpleBinder.Parameterized<?, ?>;
            if (hasParameterizedBinder != null && !hasParameterizedBinder.equals(isParameterized)) {
                throw new IllegalArgumentException(
                        "Parameterized binder and normal binder cannot be mixed"
                );
            }
            hasParameterizedBinder = isParameterized;
            binders.add(binder);
        }
        return this;
    }

    public Cache<K, V> build() {
        List<Object> binders = this.binders;
        if (binders.isEmpty()) {
            return null;
        }
        if (hasParameterizedBinder) {
            return new ParameterizedChainCacheImpl<>(binders);
        }
        return new ChainCacheImpl<>(binders);
    }
}
