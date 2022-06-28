package org.babyfish.jimmer.sql.cache;

import java.util.NavigableSet;

public interface CacheLocker {

    void lockAll(NavigableSet<String> keys);

    void unlockAll(NavigableSet<String> keys);
}
