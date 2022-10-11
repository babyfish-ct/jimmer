package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.sql.cache.Cache;

import java.util.ArrayList;
import java.util.List;

public class ChainCacheBuilder<K, V> {

    private final List<Object> binders = new ArrayList<>();

    private boolean hasParameterizedBinder = false;

    public ChainCacheBuilder<K, V> add(LoadingBinder<K, V> binder) {
        if (binder != null) {
            if (hasParameterizedBinder) {
                throw new IllegalArgumentException(
                        "The parameterized binders have been added to the builder, " +
                                "so the next binder must also be parameterized binder"
                );
            }
            binders.add(binder);
        }
        return this;
    }

    public ChainCacheBuilder<K, V> add(LoadingBinder.Parameterized<K, V> binder) {
        if (binder != null) {
            hasParameterizedBinder = true;
            binders.add(binder);
        }
        return this;
    }

    public ChainCacheBuilder<K, V> add(SimpleBinder<K, V> binder) {
        if (binder != null) {
            boolean isParameterized = binder instanceof SimpleBinder.Parameterized<?, ?>;
            if (hasParameterizedBinder && !isParameterized) {
                throw new IllegalArgumentException(
                        "The parameterized binders have been added to the builder, " +
                                "so the next binder must also be parameterized binder"
                );
            }
            hasParameterizedBinder |= isParameterized;
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
