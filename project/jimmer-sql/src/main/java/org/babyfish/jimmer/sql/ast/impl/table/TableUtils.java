package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.impl.LogicalDeletedFilterProvider;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

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

    public static boolean isRawIdAllowed(Table<?> table, JSqlClientImplementor sqlClient) {
        ImmutableProp prop = joinProp(table);
        if (prop == null) {
            return false;
        }
        if (isInverse(table)) {
            prop = prop.getOpposite();
            if (prop == null) {
                return false;
            }
        }
        if (prop.isRemote()) {
            return true;
        }
        if (!prop.isTargetForeignKeyReal(sqlClient.getMetadataStrategy())) {
            return false;
        }
        Filter<?> filter = sqlClient.getFilters().getFilter(prop.getTargetType());
        return filter == null || filter instanceof LogicalDeletedFilterProvider.IgnoredFilter;
    }
}
