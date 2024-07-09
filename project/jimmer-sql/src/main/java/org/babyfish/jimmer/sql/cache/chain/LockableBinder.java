package org.babyfish.jimmer.sql.cache.chain;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

interface LockableBinder<K, V> extends SimpleBinder<K, V> {

    KeyPrefixAwareBinder<K, V> unwrap();

    Locker locker();

    @Nullable Duration waitingDuration();

    Duration lockingDuration();

    interface Parameterized<K, V> extends SimpleBinder.Parameterized<K, V>, LockableBinder<K, V> {}
}

