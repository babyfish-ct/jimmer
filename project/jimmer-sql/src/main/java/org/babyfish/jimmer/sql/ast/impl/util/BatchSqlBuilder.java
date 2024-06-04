package org.babyfish.jimmer.sql.ast.impl.util;

import org.babyfish.jimmer.meta.ImmutableProp;

public interface BatchSqlBuilder {

    BatchSqlBuilder sql(String sql);

    BatchSqlBuilder prop(ImmutableProp prop);

    BatchSqlBuilder value(ImmutableProp prop);

    BatchSqlBuilder value(Object value);
}
