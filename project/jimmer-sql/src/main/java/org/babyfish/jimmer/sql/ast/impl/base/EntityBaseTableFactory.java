package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableFactory;

/**
 * Exposes the selected entity table while keeping its base-query owner.
 */
public enum EntityBaseTableFactory implements BaseTableFactory<Table<?>, Table<?>> {
    INSTANCE;

    @Override
    public Table<?> createNonNull(BaseTable baseTable) {
        return (Table<?>) ((BaseTableSymbol) BaseTableProxies.unwrap(baseTable)).getSelections().get(0);
    }

    @Override
    public Table<?> createNullable(BaseTable baseTable) {
        return createNonNull(baseTable);
    }
}
