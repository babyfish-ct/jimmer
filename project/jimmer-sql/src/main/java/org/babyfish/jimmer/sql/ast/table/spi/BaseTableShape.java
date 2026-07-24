package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface BaseTableShape<N extends BaseTable, Q extends BaseTable> {

    @NotNull
    static <T extends BaseTable> BaseTableShape<T, T> of(@NotNull Function<BaseTable, T> creator) {
        return new FactoryBaseTableShape<>(creator, creator);
    }

    @NotNull
    static <N extends BaseTable, Q extends BaseTable> BaseTableShape<N, Q> of(
            @NotNull Function<BaseTable, N> nonNullCreator,
            @NotNull Function<BaseTable, Q> nullableCreator
    ) {
        return new FactoryBaseTableShape<>(nonNullCreator, nullableCreator);
    }

    N createNonNull(BaseTable baseTable);

    Q createNullable(BaseTable baseTable);
}
