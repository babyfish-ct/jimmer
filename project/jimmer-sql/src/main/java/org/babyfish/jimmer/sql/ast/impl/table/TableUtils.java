package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

public class TableUtils {

    private TableUtils() {}

    public static Table<?> parent(Table<?> table) {
        if (table instanceof TableProxy<?>) {
            return ((TableProxy<?>) table).__parent();
        }
        return ((TableImplementor<?>) table).getParent();
    }

    public static boolean isInverse(Table<?> table) {
        if (table instanceof TableProxy<?>) {
            return ((TableProxy<?>) table).__isInverse();
        }
        return ((TableImplementor<?>) table).isInverse();
    }

    public static ImmutableProp joinProp(Table<?> table) {
        if (table instanceof TableProxy<?>) {
            return ((TableProxy<?>) table).__prop();
        }
        return ((TableImplementor<?>) table).getJoinProp();
    }
}
