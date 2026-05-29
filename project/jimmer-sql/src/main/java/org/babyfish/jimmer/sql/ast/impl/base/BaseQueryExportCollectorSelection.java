package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

public final class BaseQueryExportCollectorSelection extends BaseQueryExportSelection {

    BaseQueryExportCollectorSelection(BaseQueryExport export, int index, Selection<?> selection) {
        super(export, index, selection);
    }

    public int requireColumnIndex(String alias, String columnName, boolean foreignKeyInBaseQuery) {
        return export
                .requireColumn(this, tableKeys(alias), columnName, foreignKeyInBaseQuery)
                .getIndex();
    }

    public int requireJoinKeyColumnIndex(String alias, String columnName, boolean foreignKeyInBaseQuery) {
        return export
                .requireJoinKeyColumn(this, tableKeys(alias), columnName, foreignKeyInBaseQuery)
                .getIndex();
    }

    public int requireFormulaIndex(String alias, FormulaTemplate formula) {
        return export
                .requireFormula(this, tableKeys(alias), formula)
                .getIndex();
    }

    public int requireExpressionIndex() {
        return export.requireExpressionIndex(this);
    }
}
