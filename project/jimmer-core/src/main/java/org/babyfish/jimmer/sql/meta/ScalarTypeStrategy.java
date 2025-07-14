package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.EnumType;

public interface ScalarTypeStrategy {

    Class<?> getOverriddenSqlType(ImmutableProp prop);

    default EnumType.Strategy getDefaultEnumStrategy() {
        return EnumType.Strategy.NAME;
    }

}
