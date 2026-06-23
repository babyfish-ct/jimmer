package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BaseQueryExportSelection {

    protected final BaseQueryExport export;

    private final int index;

    private final RealTable rootRealTable;

    BaseQueryExportSelection(BaseQueryExport export, int index, RealTable rootRealTable) {
        this.export = export;
        this.index = index;
        this.rootRealTable = rootRealTable;
    }

    int getIndex() {
        return index;
    }

    RealTable getRealBaseTable() {
        return export.getRealBaseTable();
    }

    public boolean isTableBacked() {
        return rootRealTable != null;
    }

    public RealTable getRootRealTable() {
        return rootRealTable;
    }

    public boolean isRootTable(RealTable table) {
        if (rootRealTable == null) {
            return false;
        }
        return path(rootRealTable).equals(path(table));
    }

    public boolean containsTable(RealTable table) {
        if (rootRealTable == null) {
            return false;
        }
        List<RealTable.Key> rootPath = path(rootRealTable);
        List<RealTable.Key> tablePath = path(table);
        return isAncestorPath(tablePath, rootPath) ||
                tablePath.size() >= rootPath.size() &&
                        tablePath.subList(0, rootPath.size()).equals(rootPath);
    }

    public int columnIndex(RealTable table, String columnName, boolean foreignKeyInBaseQuery) {
        return export
                .column(this, tableKeys(table), columnName, foreignKeyInBaseQuery)
                .getIndex();
    }

    public Integer columnIndexOrNull(RealTable table, String columnName, boolean foreignKeyInBaseQuery) {
        return export.columnIndexOrNull(this, tableKeys(table), columnName, foreignKeyInBaseQuery);
    }

    public int formulaIndex(RealTable table, FormulaTemplate formula) {
        return export
                .formula(this, tableKeys(table), formula)
                .getIndex();
    }

    public int expressionIndex() {
        return export.expressionIndex(this);
    }

    public Collection<BaseQueryExportColumn> columns() {
        return export.columns(index);
    }

    protected List<RealTable.Key> tableKeys(RealTable table) {
        if (rootRealTable == null) {
            throw new IllegalStateException("Current base-query selection is not table-backed");
        }
        List<RealTable.Key> rootPath = path(rootRealTable);
        List<RealTable.Key> tablePath = path(table);
        if (isAncestorPath(tablePath, rootPath)) {
            // Foreign-key id-view exports can be selected from a joined table while
            // their physical columns are stored on that table's parent. They still
            // belong to the selected row export, whose structured path is empty.
            return new ArrayList<>();
        }
        if (tablePath.size() < rootPath.size() || !tablePath.subList(0, rootPath.size()).equals(rootPath)) {
            throw new IllegalArgumentException("The table is not inside the current base-query selection");
        }
        return new ArrayList<>(tablePath.subList(rootPath.size(), tablePath.size()));
    }

    private static boolean isAncestorPath(List<RealTable.Key> tablePath, List<RealTable.Key> rootPath) {
        return rootPath.size() >= tablePath.size() &&
                rootPath.subList(0, tablePath.size()).equals(tablePath);
    }

    private static List<RealTable.Key> path(RealTable table) {
        List<RealTable.Key> keys = new ArrayList<>();
        for (RealTable current = table; current != null; current = current.getParent()) {
            keys.add(0, current.getKey());
        }
        return keys;
    }
}
