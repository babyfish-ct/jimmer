package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface BaseTableFactory<T extends BaseTable, NT extends BaseTable> {

    @NotNull
    static <T extends BaseTable> BaseTableFactory<T, T> of(@NotNull Function<BaseTable, T> creator) {
        return new BaseTableFactoryImpl<>(creator, creator);
    }

    @NotNull
    static <T extends BaseTable, NT extends BaseTable> BaseTableFactory<T, NT> of(
            @NotNull Function<BaseTable, T> nonNullCreator,
            @NotNull Function<BaseTable, NT> nullableCreator
    ) {
        return new BaseTableFactoryImpl<>(nonNullCreator, nullableCreator);
    }

    T createNonNull(BaseTable baseTable);

    NT createNullable(BaseTable baseTable);
}
