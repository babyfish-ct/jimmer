package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;

public interface GetterMetadata {

    ImmutableProp valueProp();

    boolean isNullable();

    boolean isJson();

    boolean hasDefaultValue();

    Object getDefaultValue();

    Class<?> getSqlType();
}
