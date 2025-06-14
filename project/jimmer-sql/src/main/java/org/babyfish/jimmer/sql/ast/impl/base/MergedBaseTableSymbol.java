package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

import java.util.Set;

public interface MergedBaseTableSymbol extends BaseTableSymbol {

    Set<BaseTableSymbol> getBaseTables();

    static boolean contains(TableLike<?> table1, BaseTableSymbol table2) {
        if (table1 == table2) {
            return true;
        }
        if (table1 instanceof MergedBaseTableSymbol) {
            Set<BaseTableSymbol> baseTables = ((MergedBaseTableSymbol)table1).getBaseTables();
            return baseTables.contains(table2);
        }
        return false;
    }
}
