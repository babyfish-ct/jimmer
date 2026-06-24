package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;

final class CteTableDeclaration {

    private final RealTable table;

    private final BaseTableSymbol symbol;

    private final boolean recursive;

    CteTableDeclaration(RealTable table, BaseTableSymbol symbol, boolean recursive) {
        this.table = table;
        this.symbol = symbol;
        this.recursive = recursive;
    }

    RealTable getTable() {
        return table;
    }

    BaseTableSymbol getSymbol() {
        return symbol;
    }

    boolean isRecursive() {
        return recursive;
    }
}
