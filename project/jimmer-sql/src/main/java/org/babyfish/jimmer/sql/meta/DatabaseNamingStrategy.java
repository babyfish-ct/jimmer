package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

public interface DatabaseNamingStrategy {

    String tableName(ImmutableType type);

    String sequenceName(ImmutableType type);

    String columnName(ImmutableProp prop);

    String foreignKeyColumnName(ImmutableProp prop);

    String middleTableName(ImmutableProp prop);

    String middleTableBackRefColumnName(ImmutableProp prop);

    String middleTableTargetRefColumnName(ImmutableProp prop);
}
