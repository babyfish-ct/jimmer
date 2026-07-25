package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableFactory;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableProxy;

public class BaseTableProxies {

    private BaseTableProxies() {
    }

    public static BaseTable unwrap(BaseTable baseTable) {
        if (baseTable instanceof BaseTableProxy) {
            return ((BaseTableProxy) baseTable).__unwrap();
        }
        return baseTable;
    }

    public static BaseTable wrap(BaseTable baseTable) {
        BaseTableSymbol symbol = (BaseTableSymbol) unwrap(baseTable);
        BaseTableFactory<?, ?> factory = symbol.getBaseTableFactory();
        if (factory == null) {
            return symbol;
        }
        return factory.createNonNull(symbol);
    }
}
