package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.sql.cache.Cache;

import java.util.ArrayList;
import java.util.List;

public class ChainCacheBuilder<K, V> {

    private final List<Object> operators = new ArrayList<>();

    public ChainCacheBuilder<K, V> add(LoadingBinder<K, V> operator) {
        if (operator != null) {
            operators.add(operator);
        }
        return this;
    }

    public ChainCacheBuilder<K, V> add(SimpleBinder<K, V> operator) {
        if (operator != null) {
            operators.add(operator);
        }
        return this;
    }

    public Cache<K, V> build() {
        List<Object> ops = this.operators;
        if (ops.isEmpty()) {
            return null;
        }
        return new ChainCacheImpl<>(operators);
    }
}
