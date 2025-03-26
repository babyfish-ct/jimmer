package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.Nullable;

public class DefaultDatabaseSchemaStrategy implements DatabaseSchemaStrategy {
    @Nullable
    final String schema;

    public DefaultDatabaseSchemaStrategy() {
        this(null);
    }

    public DefaultDatabaseSchemaStrategy(@Nullable String schema) {
        this.schema = schema;
    }

    @Override
    @Nullable
    public String tableSchema(ImmutableType type) {
        return schema;
    }

    @Override
    @Nullable
    public String middleTableSchema(ImmutableProp prop) {
        return schema;
    }

    @Override
    public String toString() {
        return "DefaultDatabaseSchemaStrategy{schema='" + schema + "'}";
    }
}
