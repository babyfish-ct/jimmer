package org.babyfish.jimmer.sql.ast.impl.value;

public interface ValueGetter {

    String columnName();

    Object get(Object value);
}


