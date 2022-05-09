package org.babyfish.jimmer.sql.dialect;

public interface PaginationContext {

    int getLimit();

    int getOffset();

    PaginationContext origin();

    PaginationContext sql(String sql);

    PaginationContext variable(Object value);
}
