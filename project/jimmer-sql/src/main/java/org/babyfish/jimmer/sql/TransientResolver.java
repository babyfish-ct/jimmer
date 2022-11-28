package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.lang.Ref;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

/**
 * Only for java, kotlin developers should use `KTransientResolver`
 *
 * @param <ID>
 * @param <V>
 */
public interface TransientResolver<ID, V> {

    Map<ID, V> resolve(Collection<ID> ids, Connection con);

    default Ref<SortedMap<String, Object>> getParameterMapRef() {
        return Ref.empty();
    }
}