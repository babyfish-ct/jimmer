package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableShape;

public class BaseTableProxies {

    private BaseTableProxies() {
    }

    public static BaseTable unwrap(BaseTable baseTable) {
        if (baseTable instanceof BaseTableProxy) {
            return ((BaseTableProxy) baseTable).__unwrap();
        }
        return baseTable;
    }

    public static BaseTable wrap(BaseTable baseTable, boolean nullable) {
        BaseTableSymbol symbol = (BaseTableSymbol) unwrap(baseTable);
        BaseTableShape<?, ?> shape = symbol.getShape();
        if (shape == null) {
            return symbol;
        }
        return nullable ?
                shape.createNullable(symbol) :
                shape.createNonNull(symbol);
    }
}
