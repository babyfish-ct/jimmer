package org.babyfish.jimmer.sql.cache;

import java.util.Set;

public interface CacheLocker {

    void lockAll(Set<String> keys);

    void unlockAll(Set<String> keys);
}
