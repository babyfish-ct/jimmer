package org.babyfish.jimmer.sql.cache;

public interface UsedCache<K, V> extends Cache<K, V> {

    interface Parameterized<K, V> extends UsedCache<K, V> {}
}
