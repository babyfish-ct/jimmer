package org.babyfish.jimmer.sql.meta;

public interface SqlTypeStrategy {

    String sqlType(Class<?> elementType);

    String arrayTypeSuffix();
}
