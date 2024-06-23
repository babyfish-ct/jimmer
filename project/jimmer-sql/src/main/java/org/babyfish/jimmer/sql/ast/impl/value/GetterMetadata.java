package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.Nullable;

public interface GetterMetadata {

    ImmutableProp getValueProp();

    @Nullable
    String getColumnName();

    boolean isNullable();

    boolean isJson();

    boolean hasDefaultValue();

    Object getDefaultValue();

    Class<?> getSqlType();

    String getSqlTypeName();

    void renderTo(AbstractSqlBuilder<?> builder);
}
