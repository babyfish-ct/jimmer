package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.sql.cache.CacheLocker;

import java.time.Duration;

interface LockedBinder<K, V> {

    LockableBinder<K, V> unwrap();

    CacheLocker locker();

    Duration waitDuration();

    Duration leaseDuration();
}
