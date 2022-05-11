package org.babyfish.jimmer.sql.dialect;

public interface Dialect {

    void paginate(PaginationContext ctx);

    default UpdateJoin getUpdateJoin() {
        return null;
    }
}
