package org.babyfish.jimmer.sql.cache.chain;

import org.jetbrains.annotations.NotNull;

public interface KeyPrefixAwareBinder<K, V> extends SimpleBinder<K, V> {

    @NotNull
    String keyPrefix();

    interface Parameterized<K, V> extends KeyPrefixAwareBinder<K, V>, SimpleBinder.Parameterized<K, V> {}
}
