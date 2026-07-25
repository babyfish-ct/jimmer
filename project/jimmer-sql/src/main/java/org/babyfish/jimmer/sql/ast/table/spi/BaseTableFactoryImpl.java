package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.table.BaseTable;

import java.util.function.Function;

final class BaseTableFactoryImpl<T extends BaseTable, NT extends BaseTable> implements BaseTableFactory<T, NT> {

    private final Function<BaseTable, T> nonNullCreator;

    private final Function<BaseTable, NT> nullableCreator;

    BaseTableFactoryImpl(
            Function<BaseTable, T> nonNullCreator,
            Function<BaseTable, NT> nullableCreator
    ) {
        this.nonNullCreator = nonNullCreator;
        this.nullableCreator = nullableCreator;
    }

    @Override
    public T createNonNull(BaseTable baseTable) {
        return nonNullCreator.apply(baseTable);
    }

    @Override
    public NT createNullable(BaseTable baseTable) {
        return nullableCreator.apply(baseTable);
    }
}
