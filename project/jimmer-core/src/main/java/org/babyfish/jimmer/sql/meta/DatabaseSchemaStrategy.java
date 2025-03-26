package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.Nullable;

public interface DatabaseSchemaStrategy {

    DatabaseSchemaStrategy IMPLICIT = new DefaultDatabaseSchemaStrategy();

    @Nullable
    String tableSchema(ImmutableType type);

    @Nullable
    String middleTableSchema(ImmutableProp prop);
}
