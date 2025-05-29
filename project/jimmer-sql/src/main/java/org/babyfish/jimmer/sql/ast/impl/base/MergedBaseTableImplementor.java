package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

import java.util.Set;

public interface MergedBaseTableImplementor extends BaseTable {

    Set<BaseTableImplementor> getBaseTables();

    static boolean contains(TableLike<?> table1, BaseTableImplementor table2) {
        if (table1 == table2) {
            return true;
        }
        if (table1 instanceof MergedBaseTableImplementor) {
            Set<BaseTableImplementor> baseTables = ((MergedBaseTableImplementor)table1).getBaseTables();
            return baseTables.contains(table2);
        }
        return false;
    }
}
