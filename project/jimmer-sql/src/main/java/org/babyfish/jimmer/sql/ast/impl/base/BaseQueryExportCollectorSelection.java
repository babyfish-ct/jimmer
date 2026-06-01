package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

public final class BaseQueryExportCollectorSelection extends BaseQueryExportSelection {

    BaseQueryExportCollectorSelection(BaseQueryExport export, int index, RealTable rootRealTable) {
        super(export, index, rootRealTable);
    }

    public int requireColumnIndex(RealTable table, String columnName, boolean foreignKeyInBaseQuery) {
        return export
                .requireColumn(this, tableKeys(table), columnName, foreignKeyInBaseQuery)
                .getIndex();
    }

    public int requireJoinKeyColumnIndex(RealTable table, String columnName, boolean foreignKeyInBaseQuery) {
        return export
                .requireJoinKeyColumn(this, tableKeys(table), columnName, foreignKeyInBaseQuery)
                .getIndex();
    }

    public int requireFormulaIndex(RealTable table, FormulaTemplate formula) {
        return export
                .requireFormula(this, tableKeys(table), formula)
                .getIndex();
    }

    public int requireExpressionIndex() {
        return export.requireExpressionIndex(this);
    }
}
