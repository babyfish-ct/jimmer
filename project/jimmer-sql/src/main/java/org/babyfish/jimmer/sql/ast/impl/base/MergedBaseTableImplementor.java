package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

import java.util.Set;

public interface MergedBaseTableImplementor extends BaseTable {

    Set<BaseTableImplementor> getBaseTables();

    static boolean contains(TableLike<?> table1, BaseTable table2) {
        if (table1 == table2) {
            return true;
        }
        return table1 instanceof MergedBaseTableImplementor &&
                ((MergedBaseTableImplementor)table1).getBaseTables().contains(table2);
    }
}
