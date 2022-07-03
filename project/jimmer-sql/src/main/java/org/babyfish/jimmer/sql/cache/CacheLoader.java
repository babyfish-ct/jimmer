package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.SqlClient;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

@FunctionalInterface
public interface CacheLoader<K, V> {

    Map<K, V> loadAll(Collection<K> keys);

    static <K, V> CacheLoader<K, V> objectLoader(
            SqlClient sqlClient,
            Connection con,
            Class<V> entityType
    ) {
        return keys -> sqlClient
                .getEntities()
                .findMapByIds(entityType, keys, con);
    }
}
