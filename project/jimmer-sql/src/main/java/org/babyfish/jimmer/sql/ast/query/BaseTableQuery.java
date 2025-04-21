package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.table.BaseTable;

public interface BaseTableQuery<R, B extends BaseTable<R>> extends TypedRootQuery<R> {

    BaseTableQuery<R, B> union(TypedRootQuery<R> other);

    BaseTableQuery<R, B> unionAll(TypedRootQuery<R> other);

    BaseTableQuery<R, B> minus(TypedRootQuery<R> other);

    BaseTableQuery<R, B> intersect(TypedRootQuery<R> other);

    B asBaseTable();
}
