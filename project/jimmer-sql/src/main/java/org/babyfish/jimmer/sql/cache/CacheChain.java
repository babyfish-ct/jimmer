package org.babyfish.jimmer.sql.cache;

import java.util.Map;
import java.util.Set;

public interface CacheChain<V> {

    Map<String, V> getAll(Set<String> storageKeys);
}
