package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.ArrayList;
import java.util.List;

final class BaseQueryExportSelection {

    private final BaseQueryExport export;

    private final int index;

    private final Selection<?> selection;

    private RealTable rootRealTable;

    BaseQueryExportSelection(BaseQueryExport export, int index, Selection<?> selection) {
        this.export = export;
        this.index = index;
        this.selection = selection;
    }

    int getIndex() {
        return index;
    }

    Selection<?> getSelection() {
        return selection;
    }

    boolean isRootTable(RealTable table) {
        return path(rootRealTable()).equals(path(table));
    }

    List<RealTable.Key> tableKeys(String alias) {
        RealTable rootRealTable = rootRealTable();
        List<RealTable.Key> keys = new ArrayList<>();
        collectKeys(rootRealTable, alias, keys);
        return keys;
    }

    private RealTable rootRealTable() {
        RealTable realTable = rootRealTable;
        if (realTable == null) {
            AstContext ctx = export.astContext();
            rootRealTable = realTable = TableProxies
                    .resolve((Table<?>) selection, ctx)
                    .realTable(ctx);
        }
        return realTable;
    }

    private void collectKeys(RealTable table, String alias, List<RealTable.Key> keys) {
        if (table.getAlias().equals(alias)) {
            return;
        }
        RealTable realTable =
                table.getTableLikeImplementor()
                        .realTable(export.astContext().getJoinTypeMergeScope());
        for (RealTable childTable : realTable) {
            keys.add(childTable.getKey());
            collectKeys(childTable, alias, keys);
        }
    }

    private static List<RealTable.Key> path(RealTable table) {
        List<RealTable.Key> keys = new ArrayList<>();
        for (RealTable current = table; current.getParent() != null; current = current.getParent()) {
            keys.add(0, current.getKey());
        }
        return keys;
    }
}
