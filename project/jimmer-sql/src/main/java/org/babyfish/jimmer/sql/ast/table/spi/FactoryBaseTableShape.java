package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.table.BaseTable;

import java.util.function.Function;

final class FactoryBaseTableShape<N extends BaseTable, Q extends BaseTable> implements BaseTableShape<N, Q> {

    private final Function<BaseTable, N> nonNullCreator;

    private final Function<BaseTable, Q> nullableCreator;

    FactoryBaseTableShape(
            Function<BaseTable, N> nonNullCreator,
            Function<BaseTable, Q> nullableCreator
    ) {
        this.nonNullCreator = nonNullCreator;
        this.nullableCreator = nullableCreator;
    }

    @Override
    public N createNonNull(BaseTable baseTable) {
        return nonNullCreator.apply(baseTable);
    }

    @Override
    public Q createNullable(BaseTable baseTable) {
        return nullableCreator.apply(baseTable);
    }
}
