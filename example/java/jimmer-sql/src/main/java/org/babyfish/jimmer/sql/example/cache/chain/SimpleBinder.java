package org.babyfish.jimmer.sql.example.cache.chain;

import java.util.Collection;
import java.util.Map;

public interface SimpleBinder<K, V> {

    Map<K, V> getAll(Collection<K> keys);

    void setAll(Map<K, V> map);

    void deleteAll(Collection<K> keys);
}
