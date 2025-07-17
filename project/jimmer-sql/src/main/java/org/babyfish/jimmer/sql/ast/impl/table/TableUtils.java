package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.impl.LogicalDeletedFilterProvider;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

public class TableUtils {

    private TableUtils() {}

    public static TableLike<?> parent(TableLike<?> tableLike) {
        if (tableLike instanceof BaseTableSymbol) {
            return ((BaseTableSymbol)tableLike).getParent();
        }
        return parent((Table<?>) tableLike);
    }

    public static Table<?> parent(Table<?> table) {
        if (table instanceof TableProxy<?>) {
            return ((TableProxy<?>) table).__parent();
        }
        return ((TableImplementor<?>) table).getParent();
    }

    public static boolean hasBaseTable(TableLikeImplementor<?> tableLike) {
        if (tableLike instanceof BaseTableImplementor) {
            return true;
        }
        return tableLike.hasBaseTable();
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

    public static Table<?> disableJoin(Table<?> table, String reason) {
        if (table instanceof TableImplementor<?>) {
            return new UntypedJoinDisabledTableProxy<>((TableImplementor<?>) table, reason);
        }
        return ((TableProxy<?>) table).__disableJoin(reason);
    }
}
