package org.babyfish.jimmer.sql.ast.impl.value;

public interface GetterMetadata {

    boolean isJson();

    Object getDefaultValue();

    Class<?> getSqlType();
}
