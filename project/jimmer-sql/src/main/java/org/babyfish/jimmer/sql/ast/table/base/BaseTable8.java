package org.babyfish.jimmer.sql.ast.table.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.jetbrains.annotations.NotNull;

public interface BaseTable8<
        S1 extends Selection<?>,
        S2 extends Selection<?>,
        S3 extends Selection<?>,
        S4 extends Selection<?>,
        S5 extends Selection<?>,
        S6 extends Selection<?>,
        S7 extends Selection<?>,
        S8 extends Selection<?>
> extends BaseTable {

    @NotNull
    S1 get_1();

    @NotNull
    S2 get_2();

    @NotNull
    S3 get_3();

    @NotNull
    S4 get_4();

    @NotNull
    S5 get_5();

    @NotNull
    S6 get_6();

    @NotNull
    S7 get_7();

    @NotNull
    S8 get_8();
}
