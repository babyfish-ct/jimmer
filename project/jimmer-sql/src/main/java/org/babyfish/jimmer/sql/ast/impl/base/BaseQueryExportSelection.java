package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

import java.util.Collection;
import java.util.ArrayList;
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

    public String getAlias() {
        return export.getRealBaseTable().getAlias();
    }

    public boolean isRootTable(RealTable table) {
        return path(rootRealTable).equals(path(table));
    }

    public int columnIndex(String alias, String columnName, boolean foreignKeyInBaseQuery) {
        return export
                .column(this, tableKeys(alias), columnName, foreignKeyInBaseQuery)
                .getIndex();
    }

    public Integer columnIndexOrNull(String alias, String columnName, boolean foreignKeyInBaseQuery) {
        return export.columnIndexOrNull(this, tableKeys(alias), columnName, foreignKeyInBaseQuery);
    }

    public int formulaIndex(String alias, FormulaTemplate formula) {
        return export
                .formula(this, tableKeys(alias), formula)
                .getIndex();
    }

    public int expressionIndex() {
        return export.expressionIndex(this);
    }

    public Collection<BaseQueryExportColumn> columns() {
        return export.columns(index);
    }

    protected List<RealTable.Key> tableKeys(String alias) {
        List<RealTable.Key> keys = new ArrayList<>();
        collectKeys(rootRealTable, alias, keys);
        return keys;
    }

    private void collectKeys(RealTable table, String alias, List<RealTable.Key> keys) {
        if (table.getAlias().equals(alias)) {
            return;
        }
        for (RealTable childTable : table) {
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
