package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.table.BaseTable;

public interface BaseTableShape<N extends BaseTable, Q extends BaseTable> {

    N createNonNull(BaseTable baseTable);

    Q createNullable(BaseTable baseTable);
}
