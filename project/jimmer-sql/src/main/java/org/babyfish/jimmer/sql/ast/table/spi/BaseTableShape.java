package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.table.BaseTable;

public interface BaseTableShape<N, Q> {

    N createNonNull(BaseTable baseTable);

    Q createNullable(BaseTable baseTable);
}
