package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.meta.ImmutableProp;

public interface ScalarTypeStrategy {

    Class<?> getOverriddenSqlType(ImmutableProp prop);
}
