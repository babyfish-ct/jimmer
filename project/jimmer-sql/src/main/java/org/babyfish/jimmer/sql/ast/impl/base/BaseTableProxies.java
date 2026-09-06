package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableFactory;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

public class BaseTableProxies {

    private BaseTableProxies() {
    }

    public static BaseTable unwrap(BaseTable baseTable) {
        if (baseTable instanceof BaseTableProxy) {
            return ((BaseTableProxy) baseTable).__unwrap();
        }
        return baseTable;
    }

    public static BaseTable resolve(TableLike<?> table) {
        if (table instanceof BaseTable) {
            return unwrap((BaseTable) table);
        }
        BaseTableOwner owner = BaseTableOwner.of(table);
        if (owner != null) {
            return owner.getBaseTable();
        }
        throw new IllegalArgumentException("source must be a typed base-table symbol or a table selected by a base query");
    }

    @SuppressWarnings("unchecked")
    public static <T extends TableLike<?>> T wrap(BaseTable baseTable) {
        BaseTableSymbol symbol = (BaseTableSymbol) unwrap(baseTable);
        BaseTableFactory<?, ?> factory = symbol.getBaseTableFactory();
        if (factory == null) {
            return (T) symbol;
        }
        return (T) factory.createNonNull(symbol);
    }
}
