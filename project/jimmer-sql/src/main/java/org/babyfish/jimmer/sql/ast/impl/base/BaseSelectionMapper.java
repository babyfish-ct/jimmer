package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

import java.util.Collection;
import java.util.List;

public class BaseSelectionMapper {

    private final BaseQueryExport export;

    private final BaseQueryExportSelection selection;

    int expressionIndex;

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
        if (expressionIndex == 0) {
            expressionIndex = export.nextColumnIndex();
        }
        return expressionIndex;
    }

    Collection<BaseQueryExportColumn> columns() {
        return export.columns(selection.getIndex());
    }
}
