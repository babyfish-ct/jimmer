package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.impl.query.BaseTableQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.jetbrains.annotations.NotNull;

public interface BaseTableImplementor<T> extends BaseTable<T>, TableLikeImplementor<T> {

    BaseTableQueryImplementor<T, ?> getQuery();
}
