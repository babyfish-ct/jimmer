package org.babyfish.jimmer.sql.ast.table.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.jetbrains.annotations.NotNull;

public interface BaseTable2<S1 extends Selection<?>, S2 extends Selection<?>> extends BaseTable {

    @NotNull
    S1 get_1();

    @NotNull
    S2 get_2();
}
