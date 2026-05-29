package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

public final class BaseQueryExportCollectorSelection extends BaseQueryExportSelection {

    BaseQueryExportCollectorSelection(BaseQueryExport export, int index, Selection<?> selection) {
        super(export, index, selection);
    }

    @Override
    public int columnIndex(String alias, String columnName, boolean foreignKeyInBaseQuery) {
        return export
                .requireColumn(this, tableKeys(alias), columnName, foreignKeyInBaseQuery)
                .getIndex();
    }

    @Override
    public int joinKeyColumnIndex(String alias, String columnName, boolean foreignKeyInBaseQuery) {
        return export
                .requireJoinKeyColumn(this, tableKeys(alias), columnName, foreignKeyInBaseQuery)
                .getIndex();
    }

    @Override
    public int formulaIndex(String alias, FormulaTemplate formula) {
        return export
                .requireFormula(this, tableKeys(alias), formula)
                .getIndex();
    }

    @Override
    public int expressionIndex() {
        return export.requireExpressionIndex(this);
    }
}
