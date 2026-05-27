package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

import java.util.*;

public class BaseSelectionMapper {

    private final BaseQueryExport export;

    private final int selectionIndex;

    final Map<BaseQueryExportColumn.Key, BaseQueryExportColumn> columnMap = new LinkedHashMap<>();

    int expressionIndex;

    BaseSelectionMapper(BaseQueryExport export, int selectionIndex) {
        this.export = export;
        this.selectionIndex = selectionIndex;
    }

    public String getAlias() {
        return export.getRealBaseTable().getAlias();
    }

    public int columnIndex(String alias, String columnName, boolean foreignKeyInBaseQuery) {
        AstContext ctx = export.astContext();
        Selection<?> selection = ((BaseTableImplementor) export
                .getRealBaseTable()
                .getTableLikeImplementor())
                .getSelections()
                .get(selectionIndex);
        RealTable realTable = TableProxies.resolve((Table<?>) selection, ctx).realTable(ctx);
        List<RealTable.Key> keys = keys(realTable, alias);
        BaseQueryExportColumn.Key key =
                new BaseQueryExportColumn.Key(keys, columnName, null, foreignKeyInBaseQuery);
        return columnMap.computeIfAbsent(
                key,
                it -> new BaseQueryExportColumn(
                        keys,
                        columnName,
                        foreignKeyInBaseQuery,
                        export.nextColumnIndex()
                )
        ).getIndex();
    }

    public int formulaIndex(String alias, FormulaTemplate formula) {
        AstContext ctx = export.astContext();
        Selection<?> selection = ((BaseTableImplementor) export
                .getRealBaseTable()
                .getTableLikeImplementor())
                .getSelections()
                .get(selectionIndex);
        RealTable realTable = TableProxies.resolve((Table<?>) selection, ctx).realTable(ctx);
        List<RealTable.Key> keys = keys(realTable, alias);
        BaseQueryExportColumn.Key key =
                new BaseQueryExportColumn.Key(keys, null, formula, false);
        return columnMap.computeIfAbsent(
                key,
                it -> new BaseQueryExportColumn(
                        keys,
                        formula,
                        export.nextColumnIndex()
                )
        ).getIndex();
    }

    public int expressionIndex() {
        if (expressionIndex == 0) {
            expressionIndex = export.nextColumnIndex();
        }
        return expressionIndex;
    }

    private List<RealTable.Key> keys(RealTable table, String alias) {
        List<RealTable.Key> keys = new ArrayList<>();
        keys0(table, alias, keys);
        return keys;
    }

    private void keys0(RealTable table, String alias, List<RealTable.Key> keys) {
        if (table.getAlias().equals(alias)) {
            return;
        }
        RealTable realTable =
                (table.getTableLikeImplementor())
                        .realTable(export.astContext().getJoinTypeMergeScope());
        for (RealTable childTable : realTable) {
            keys.add(childTable.getKey());
            keys0(childTable, alias, keys);
        }
    }

    Collection<BaseQueryExportColumn> columns() {
        return columnMap.values();
    }
}
