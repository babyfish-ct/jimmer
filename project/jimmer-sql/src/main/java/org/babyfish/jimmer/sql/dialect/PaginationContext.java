package org.babyfish.jimmer.sql.dialect;

public interface PaginationContext {

    int getLimit();

    long getOffset();

    boolean isIdOnly();

    PaginationContext origin();

    PaginationContext space();

    PaginationContext newLine();

    PaginationContext sql(String sql);

    PaginationContext variable(Object value);
}
