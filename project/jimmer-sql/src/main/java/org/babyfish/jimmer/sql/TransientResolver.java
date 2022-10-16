package org.babyfish.jimmer.sql;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

public interface TransientResolver<ID, V> {

    Map<ID, V> resolve(Collection<ID> ids, Connection con);

    interface Parameterized<ID, V> extends TransientResolver<ID, V> {

        NavigableMap<String, Object> getParameters();
    }
}