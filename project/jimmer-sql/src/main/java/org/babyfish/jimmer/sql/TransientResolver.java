package org.babyfish.jimmer.sql;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

public interface TransientResolver<ID, V> {

    Map<ID, V> resolve(Collection<ID> ids, Connection con);
}