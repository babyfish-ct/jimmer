package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

public interface ConfigurableBaseTableQuery<T extends TableLike<?>, R, B extends BaseTable<R>>
extends BaseTableQuery<R, B>, ConfigurableRootQuery<T, R> {
}
