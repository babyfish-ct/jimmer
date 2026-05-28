package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

import java.util.Collection;
import java.util.List;

public class BaseSelectionMapper {

    private final BaseQueryExport export;

    private final BaseQueryExportSelection selection;

    BaseSelectionMapper(BaseQueryExport export, BaseQueryExportSelection selection) {
        this.export = export;
        this.selection = selection;
    }

    public String getAlias() {
        return export.getRealBaseTable().getAlias();
    }

    public int columnIndex(String alias, String columnName, boolean foreignKeyInBaseQuery) {
        List<RealTable.Key> keys = selection.tableKeys(alias);
        return export
                .column(selection, keys, columnName, foreignKeyInBaseQuery)
                .getIndex();
    }

    public Integer columnIndexOrNull(String alias, String columnName, boolean foreignKeyInBaseQuery) {
        List<RealTable.Key> keys = selection.tableKeys(alias);
        return export.columnIndexOrNull(selection, keys, columnName, foreignKeyInBaseQuery);
    }

    public int joinKeyColumnIndex(String alias, String columnName, boolean foreignKeyInBaseQuery) {
        List<RealTable.Key> keys = selection.tableKeys(alias);
        return export
                .joinKeyColumn(selection, keys, columnName, foreignKeyInBaseQuery)
                .getIndex();
    }

    public int formulaIndex(String alias, FormulaTemplate formula) {
        List<RealTable.Key> keys = selection.tableKeys(alias);
        return export
                .formula(selection, keys, formula)
                .getIndex();
    }

    public int expressionIndex() {
        return export.expressionIndex(selection);
    }

    public boolean isRootTable(RealTable table) {
        return selection.isRootTable(table);
    }

    Collection<BaseQueryExportColumn> columns() {
        return export.columns(selection.getIndex());
    }
}
